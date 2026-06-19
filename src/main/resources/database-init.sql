-- 1. Structural resets
DROP TABLE IF EXISTS sf_ways CASCADE;
DROP TABLE IF EXISTS sf_ways_vertices_pgr CASCADE;
DROP TABLE IF EXISTS planet_osm_line CASCADE;
DROP TABLE IF EXISTS stations CASCADE;
DROP TABLE IF EXISTS drones CASCADE;
DROP TABLE IF EXISTS orders CASCADE;

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

--- orders
CREATE TABLE orders(
                       id BIGSERIAL PRIMARY KEY,
                       order_no TEXT NOT NULL UNIQUE,
                       station_id BIGINT NOT NULL,
                       assigned_drone_id BIGINT,
                       delivery_position GEOGRAPHY(POINT,4326) NOT NULL,
                       status SMALLINT NOT NULL DEFAULT 0,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       assigned_at TIMESTAMP,
                       delivered_at TIMESTAMP,
                       completed_at TIMESTAMP,
                       CONSTRAINT fk_order_station
                           FOREIGN KEY(station_id)
                               REFERENCES stations(id),
                       CONSTRAINT fk_order_drone
                           FOREIGN KEY(assigned_drone_id)
                               REFERENCES drones(id)
);

