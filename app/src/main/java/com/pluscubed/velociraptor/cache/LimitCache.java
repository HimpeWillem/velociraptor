package com.pluscubed.velociraptor.cache;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;

import com.pluscubed.velociraptor.api.LimitResponse;
import com.pluscubed.velociraptor.api.osmapi.Coord;
import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.SqlBrite;
import com.squareup.sqldelight.SqlDelightStatement;

import java.util.List;

import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class LimitCache {

    private static LimitCache instance;

    private final BriteDatabase db;
    private final LimitCacheWay.Put put;
    private final LimitCacheWay.Cleanup cleanup;
    private final LimitCacheWay.Update_way update;

    LimitCache(Context context, Scheduler scheduler) {
        SqlBrite sqlBrite = new SqlBrite.Builder().build();
        LimitCacheSqlHelper helper = new LimitCacheSqlHelper(context);
        db = sqlBrite.wrapDatabaseHelper(helper, scheduler);
        SQLiteDatabase writableDatabase = db.getWritableDatabase();

        put = new LimitCacheWay.Put(writableDatabase);
        update = new LimitCacheWay.Update_way(writableDatabase);
        cleanup = new LimitCacheWay.Cleanup(writableDatabase);
    }


    public static LimitCache getInstance(Context context) {
        if (instance == null) {
            instance = new LimitCache(context, Schedulers.io());
        }
        return instance;
    }

    private static double crossTrackDist(Coord p1, Coord p2, Coord t) {
        Location a = p1.toLocation();
        Location b = p2.toLocation();
        Location x = t.toLocation();

        return Math.abs(Math.asin(Math.sin(a.distanceTo(x) / 6371008) * Math.sin(Math.toRadians(a.bearingTo(x) - a.bearingTo(b)))) * 6371008);
    }

    public void put(LimitResponse response) {
        if (response.coords().isEmpty()) {
            return;
        }

        BriteDatabase.Transaction transaction = db.newTransaction();
        try {
            List<LimitCacheWay> ways = LimitCacheWay.fromResponse(response);
            for (LimitCacheWay way : ways) {
                update.bind(way.clat(), way.clon(), way.maxspeed(), way.timestamp(), way.lat1(), way.lon1(), way.lat2(), way.lon2(), way.road());
                int rowsAffected = db.executeUpdateDelete(update.table, update.program);

                if (rowsAffected != 1) {
                    put.bind(way.clat(), way.clon(), way.maxspeed(), way.timestamp(), way.lat1(), way.lon1(), way.lat2(), way.lon2(), way.road());
                    long rowId = db.executeInsert(put.table, put.program);
                    Timber.d("Cache put: " + rowId + " - " + way.toString());
                }
            }

            transaction.markSuccessful();
        } finally {
            transaction.end();
        }
    }

    public Observable<LimitResponse> get(final String previousName, final Coord coord) {
        return Observable.defer(() -> {
            LimitCache.this.cleanup();

            SqlDelightStatement selectStatement = LimitCacheWay.FACTORY.select_by_coord(coord.lat, Math.pow(Math.cos(Math.toRadians(coord.lat)), 2), coord.lon);

            return db.createQuery(selectStatement.tables, selectStatement.statement, selectStatement.args)
                    .mapToList(LimitCacheWay.SELECT_BY_COORD::map)
                    .take(1)
                    .flatMap(Observable::from)
                    .filter(way -> {
                        Coord coord1 = new Coord(way.lat1(), way.lon1());
                        Coord coord2 = new Coord(way.lat2(), way.lon2());
                        double crossTrackDist = crossTrackDist(coord1, coord2, coord);
                        return crossTrackDist < 15 /*&& isOnSegment(coord1, coord2, coord)*/;
                    })
                    .toList()
                    .flatMap(ways -> {
                        if (ways.isEmpty()) {
                            return Observable.empty();
                        }

                        LimitResponse.Builder response = ways.get(0).toResponse();
                        for (LimitCacheWay way : ways) {
                            if (way.road() != null && way.road().equals(previousName)) {
                                response = way.toResponse();
                                break;
                            }
                        }

                        response.setFromCache(true);
                        return Observable.just(response.build());
                    });
        });

    }


    private void cleanup() {
        cleanup.bind(System.currentTimeMillis());
        db.executeUpdateDelete(cleanup.table, cleanup.program);
    }
}
