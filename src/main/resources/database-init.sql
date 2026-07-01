-- 1. Structural resets
DROP TABLE IF EXISTS order_items CASCADE;
DROP TABLE IF EXISTS station_products CASCADE;
DROP TABLE IF EXISTS products CASCADE;
DROP TABLE IF EXISTS sf_ways CASCADE;
DROP TABLE IF EXISTS sf_ways_vertices_pgr CASCADE;
DROP TABLE IF EXISTS planet_osm_line CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS drones CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS stations CASCADE;

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
                      email TEXT NOT NULL UNIQUE,
                      password_hash TEXT NOT NULL
);

--- products (global catalog)
CREATE TABLE products(
                         id BIGSERIAL PRIMARY KEY,
                         name TEXT NOT NULL,
                         description TEXT,
                         price DECIMAL(10, 2) NOT NULL,
                         image_url TEXT
);

--- per-station inventory
CREATE TABLE station_products(
                                 station_id BIGINT NOT NULL,
                                 product_id BIGINT NOT NULL,
                                 stock INTEGER NOT NULL DEFAULT 0,
                                 PRIMARY KEY (station_id, product_id),
                                 CONSTRAINT fk_station_product_station
                                     FOREIGN KEY(station_id)
                                         REFERENCES stations(id),
                                 CONSTRAINT fk_station_product_product
                                     FOREIGN KEY(product_id)
                                         REFERENCES products(id)
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
                       total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0,
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

--- order line items
CREATE TABLE order_items(
                            id BIGSERIAL PRIMARY KEY,
                            order_id BIGINT NOT NULL,
                            product_id BIGINT NOT NULL,
                            quantity INTEGER NOT NULL,
                            unit_price DECIMAL(10, 2) NOT NULL,
                            CONSTRAINT fk_order_item_order
                                FOREIGN KEY(order_id)
                                    REFERENCES orders(id),
                            CONSTRAINT fk_order_item_product
                                FOREIGN KEY(product_id)
                                    REFERENCES products(id)
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

-- Default password for seed users: password123
INSERT INTO users (name, address, email, password_hash) VALUES
    ('Alice Chen', '100 Valencia St, San Francisco, CA', 'alice.chen@example.com', '$2b$10$FS6TlN2qQzEkp9EE2rcZC.EWfjOUaQPUOzWU2Q623RNLCzM8yn4cO'),
    ('Bob Martinez', '500 Brannan St, San Francisco, CA', 'bob.martinez@example.com', '$2b$10$FS6TlN2qQzEkp9EE2rcZC.EWfjOUaQPUOzWU2Q623RNLCzM8yn4cO');

INSERT INTO products (name, description, price, image_url) VALUES
    ('Organic Avocados (3-pack)', 'Fresh California avocados', 8.99,
     'https://images.unsplash.com/photo-1523049673857-ae8efb4241e8?w=400&h=400&fit=crop'),
    ('Sourdough Bread', 'Artisan San Francisco sourdough loaf', 6.50,
     'https://images.unsplash.com/photo-1509440159596-023109603a77?w=400&h=400&fit=crop'),
    ('Cold Brew Coffee (32oz)', 'Locally roasted cold brew', 5.99,
     'https://images.unsplash.com/photo-1517701554657-f96066381705?w=400&h=400&fit=crop'),
    ('Fresh Salmon Fillet', 'Wild-caught Pacific salmon', 18.99,
     'https://images.unsplash.com/photo-1519708227418-f1807756a880?w=400&h=400&fit=crop'),
    ('Mixed Greens Salad Kit', 'Organic salad with dressing', 7.49,
     'https://images.unsplash.com/photo-1546066621-842cac658919?w=400&h=400&fit=crop'),
    ('Sparkling Water (12-pack)', 'Premium sparkling mineral water', 9.99,
     'https://images.unsplash.com/photo-1523362628746-0c100150b504?w=400&h=400&fit=crop'),
    ('Matcha Latte Kit', 'Ceremonial grade matcha with oat milk', 12.99,
     'https://images.unsplash.com/photo-1556678893-371087474945?w=400&h=400&fit=crop'),
    ('Vegan Burrito Bowl', 'Rice, beans, guacamole, and salsa', 11.49,
     'https://images.unsplash.com/photo-1626700051175-6816013ee784?w=400&h=400&fit=crop'),
    ('Dark Chocolate Bar', '70% cacao single-origin chocolate', 4.99,
     'https://images.unsplash.com/photo-1481391319763-054352947d67?w=400&h=400&fit=crop');

-- Every station stocks every product
INSERT INTO station_products (station_id, product_id, stock)
SELECT s.id, p.id, CASE p.name
    WHEN 'Organic Avocados (3-pack)' THEN 50
    WHEN 'Sourdough Bread' THEN 30
    WHEN 'Cold Brew Coffee (32oz)' THEN 40
    WHEN 'Fresh Salmon Fillet' THEN 20
    WHEN 'Mixed Greens Salad Kit' THEN 35
    WHEN 'Sparkling Water (12-pack)' THEN 60
    WHEN 'Matcha Latte Kit' THEN 25
    WHEN 'Vegan Burrito Bowl' THEN 45
    WHEN 'Dark Chocolate Bar' THEN 80
    ELSE 20
END
FROM stations s
CROSS JOIN products p;
