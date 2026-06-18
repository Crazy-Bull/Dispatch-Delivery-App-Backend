-- 1. Structural resets
DROP TABLE IF EXISTS sf_ways CASCADE;
DROP TABLE IF EXISTS sf_ways_vertices_pgr CASCADE;
DROP TABLE IF EXISTS planet_osm_line CASCADE;

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