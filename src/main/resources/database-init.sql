-- 1. Structural resets
DROP TABLE IF EXISTS sf_ways CASCADE;
DROP TABLE IF EXISTS sf_ways_vertices_pgr CASCADE;
DROP TABLE IF EXISTS planet_osm_line CASCADE;
DROP TABLE IF EXISTS stations CASCADE;
DROP TABLE IF EXISTS drones CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- 2. Extensions provisioning
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pgrouting;

-- 3. Production routing sink table
CREATE TABLE sf_ways (
                         gid SERIAL PRIMARY KEY,
                         osm_id BIGINT,
                         name TEXT,
                         highway TEXT,
                         maxspeed INTEGER DEFAULT 30,
                         one_way VARCHAR(3),
                         source INTEGER,
                         target INTEGER,
                         cost DOUBLE PRECISION,
                         reverse_cost DOUBLE PRECISION,
                         geom geometry(LineString, 4326)
);

CREATE INDEX idx_sf_ways_geom ON sf_ways USING gist(geom);


--- stations

CREATE TABLE stations(
                         id BIGSERIAL PRIMARY KEY,
                         name TEXT NOT NULL,
                         position GEOGRAPHY(POINT,4326) NOT NULL,
                         address TEXT
);

--- Drones and tracking system
CREATE TABLE drones(
                       id BIGSERIAL PRIMARY KEY,
                       drone_code TEXT NOT NULL UNIQUE,
                       station_id BIGINT NOT NULL,
                       battery_level INTEGER NOT NULL DEFAULT 100,
                       position GEOGRAPHY(POINT,4326) NOT NULL,
                       altitude DOUBLE PRECISION NOT NULL DEFAULT 0,
                       speed DOUBLE PRECISION NOT NULL DEFAULT 0,
                       status SMALLINT NOT NULL DEFAULT 0,
                       CONSTRAINT fk_drone_station
                           FOREIGN KEY(station_id)
                               REFERENCES stations(id)
);

--- users
CREATE TABLE users(
                      id BIGSERIAL PRIMARY KEY,
                      name TEXT NOT NULL,
                      address TEXT NOT NULL,
                      email TEXT NOT NULL UNIQUE
);

--- orders
CREATE TABLE orders(
                       id BIGSERIAL PRIMARY KEY,
                       order_no TEXT NOT NULL UNIQUE,
                       user_id BIGINT NOT NULL,
                       station_id BIGINT NOT NULL,
                       assigned_drone_id BIGINT,
                       delivery_position GEOGRAPHY(POINT,4326) NOT NULL,
                       status SMALLINT NOT NULL DEFAULT 0,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       assigned_at TIMESTAMP,
                       delivered_at TIMESTAMP,
                       completed_at TIMESTAMP,
                       CONSTRAINT fk_order_user
                           FOREIGN KEY(user_id)
                               REFERENCES users(id),
                       CONSTRAINT fk_order_station
                           FOREIGN KEY(station_id)
                               REFERENCES stations(id),
                       CONSTRAINT fk_order_drone
                           FOREIGN KEY(assigned_drone_id)
                               REFERENCES drones(id)
);

-- 4. Seed data: 3 stations and 2 drones per station
INSERT INTO stations (name, position, address) VALUES
    ('Mission Hub', ST_GeogFromText('SRID=4326;POINT(-122.4194 37.7749)'), '1 Market St, San Francisco, CA'),
    ('Marina Hub', ST_GeogFromText('SRID=4326;POINT(-122.4367 37.8024)'), '2001 Chestnut St, San Francisco, CA'),
    ('Sunset Hub', ST_GeogFromText('SRID=4326;POINT(-122.4862 37.7599)'), '2400 Judah St, San Francisco, CA');

INSERT INTO drones (drone_code, station_id, battery_level, position, altitude, speed, status)
SELECT 'DRONE-M1', id, 100, position, 0, 0, 0 FROM stations WHERE name = 'Mission Hub'
UNION ALL
SELECT 'DRONE-M2', id, 100, position, 0, 0, 0 FROM stations WHERE name = 'Mission Hub'
UNION ALL
SELECT 'DRONE-A1', id, 100, position, 0, 0, 0 FROM stations WHERE name = 'Marina Hub'
UNION ALL
SELECT 'DRONE-A2', id, 100, position, 0, 0, 0 FROM stations WHERE name = 'Marina Hub'
UNION ALL
SELECT 'DRONE-S1', id, 100, position, 0, 0, 0 FROM stations WHERE name = 'Sunset Hub'
UNION ALL
SELECT 'DRONE-S2', id, 100, position, 0, 0, 0 FROM stations WHERE name = 'Sunset Hub';

INSERT INTO users (name, address, email) VALUES
    ('Alice Chen', '100 Valencia St, San Francisco, CA', 'alice.chen@example.com'),
    ('Bob Martinez', '500 Brannan St, San Francisco, CA', 'bob.martinez@example.com');

