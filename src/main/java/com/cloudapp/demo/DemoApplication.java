package com.cloudapp.demo;



import com.google.cloud.spanner.*;
import com.google.spanner.admin.database.v1.CreateDatabaseMetadata;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SpringBootApplication
public class DemoApplication {

    static class Singer {

        final long singerId;
        final String firstName;
        final String lastName;

        Singer(long singerId, String firstName, String lastName) {
            this.singerId = singerId;
            this.firstName = firstName;
            this.lastName = lastName;
        }
    }

    /**
     * Class to contain album sample data.
     */
    static class Album {

        final long singerId;
        final long albumId;
        final String albumTitle;

        Album(long singerId, long albumId, String albumTitle) {
            this.singerId = singerId;
            this.albumId = albumId;
            this.albumTitle = albumTitle;
        }
    }

    /**
     * Class to contain performance sample data.
     */
    static class Performance {

        final long singerId;
        final long venueId;
        final String eventDate;
        final long revenue;

        Performance(long singerId, long venueId, String eventDate, long revenue) {
            this.singerId = singerId;
            this.venueId = venueId;
            this.eventDate = eventDate;
            this.revenue = revenue;
        }
    }

    static final List<Singer> SINGERS =
            Arrays.asList(
                    new Singer(1, "Marc", "Richards"),
                    new Singer(2, "Catalina", "Smith"),
                    new Singer(3, "Alice", "Trentor"),
                    new Singer(4, "Lea", "Martin"),
                    new Singer(5, "David", "Lomond"));

    static final List<Album> ALBUMS =
            Arrays.asList(
                    new Album(1, 1, "Total Junk"),
                    new Album(1, 2, "Go, Go, Go"),
                    new Album(2, 1, "Green"),
                    new Album(2, 2, "Forever Hold Your Peace"),
                    new Album(2, 3, "Terrified"));

    static void writeExampleData(DatabaseClient dbClient) {
        List<Mutation> mutations = new ArrayList<>();
        for (Singer singer : SINGERS) {
            mutations.add(
                    Mutation.newInsertBuilder("Singers")
                            .set("SingerId")
                            .to(singer.singerId)
                            .set("FirstName")
                            .to(singer.firstName)
                            .set("LastName")
                            .to(singer.lastName)
                            .build());
        }
        for (Album album : ALBUMS) {
            mutations.add(
                    Mutation.newInsertBuilder("Albums")
                            .set("SingerId")
                            .to(album.singerId)
                            .set("AlbumId")
                            .to(album.albumId)
                            .set("AlbumTitle")
                            .to(album.albumTitle)
                            .build());
        }
        dbClient.write(mutations);
    }

    static void query(DatabaseClient dbClient) {
        // singleUse() can be used to execute a single read or query against Cloud Spanner.
        ResultSet resultSet =
                dbClient
                        .singleUse()
                        .executeQuery(Statement.of("SELECT SingerId, AlbumId, AlbumTitle FROM Albums"));
        while (resultSet.next()) {
            System.out.printf(
                    "%d %d %s\n", resultSet.getLong(0), resultSet.getLong(1), resultSet.getString(2));
        }
    }


    static void read(DatabaseClient dbClient) {
        ResultSet resultSet =
                dbClient
                        .singleUse()
                        .read("Albums",
                                // KeySet.all() can be used to read all rows in a table. KeySet exposes other
                                // methods to read only a subset of the table.
                                KeySet.all(),
                                Arrays.asList("SingerId", "AlbumId", "AlbumTitle"));
        while (resultSet.next()) {
            System.out.printf(
                    "%d %d %s\n", resultSet.getLong(0), resultSet.getLong(1), resultSet.getString(2));
        }
    }


	public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);

        SpannerOptions options = SpannerOptions.newBuilder().build();
        Spanner spanner = options.getService();
        try {
            String command = args[0];
            DatabaseId db = DatabaseId.of(options.getProjectId(), args[1], args[2]);
            DatabaseClient dbClient = spanner.getDatabaseClient(db);
            DatabaseAdminClient dbAdminClient = spanner.getDatabaseAdminClient();

        } finally {
            System.out.print("");
        }



    }





	static void createDatabase(DatabaseAdminClient dbAdminClient, DatabaseId id) {

	    Operation<Database, CreateDatabaseMetadata> op;
        op = dbAdminClient
                .createDatabase(
                        id.getInstanceId().getInstance(),
                        id.getDatabase(),
                        Arrays.asList(
                                "CREATE TABLE Singers (\n"
                                        + "  SingerId   INT64 NOT NULL,\n"
                                        + "  FirstName  STRING(1024),\n"
                                        + "  LastName   STRING(1024),\n"
                                        + "  SingerInfo BYTES(MAX)\n"
                                        + ") PRIMARY KEY (SingerId)",
                                "CREATE TABLE Albums (\n"
                                        + "  SingerId     INT64 NOT NULL,\n"
                                        + "  AlbumId      INT64 NOT NULL,\n"
                                        + "  AlbumTitle   STRING(MAX)\n"
                                        + ") PRIMARY KEY (SingerId, AlbumId),\n"
                                        + "  INTERLEAVE IN PARENT Singers ON DELETE CASCADE"));
        Database db = op.waitFor().getResult();
		System.out.println("Created database [" + db.getId() + "]");

	}


}
