DROP TABLE IF EXISTS city CASCADE;
DROP TABLE IF EXISTS coordinates CASCADE;
DROP TABLE IF EXISTS human CASCADE;

DO
$$
    BEGIN
        IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'climate') THEN
            DROP TYPE climate;
        END IF;
        IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'government') THEN
            DROP TYPE government;
        END IF;
    END
$$;

CREATE TYPE climate AS ENUM
    ('RAIN_FOREST', 'HUMIDSUBTROPICAL', 'TUNDRA');


CREATE TYPE government AS ENUM
    ('DEMARCHY', 'KLEPTOCRACY', 'CORPORATOCRACY', 'PLUTOCRACY', 'THALASSOCRACY');



CREATE TABLE coordinates
(
    id BIGSERIAL PRIMARY KEY,
    x  REAL NOT NULL CHECK (x <= 460),
    y  REAL NOT NULL
);

CREATE TABLE human
(
    id     BIGSERIAL PRIMARY KEY,
    height REAL NOT NULL CHECK (height > 0)
);

CREATE TABLE city
(
    id                     BIGSERIAL PRIMARY KEY,

    name                   VARCHAR(255) NOT NULL,
    CONSTRAINT city_name_not_blank CHECK (length(btrim(name)) > 0),

    creation_date          DATE         NOT NULL DEFAULT CURRENT_DATE,
    area                   INTEGER      NOT NULL CHECK (area > 0),
    population             BIGINT       NOT NULL CHECK (population > 0),

    establishment_date     DATE,
    capital                BOOLEAN      NOT NULL,

    meters_above_sea_level INTEGER,

    telephone_code         INTEGER      NOT NULL CHECK (telephone_code > 0 AND telephone_code <= 100000),

    climate                climate      NOT NULL,
    government             government,

    coordinates_id         BIGINT    NOT NULL,
    governor_id            BIGINT,

    CONSTRAINT fk_city_coordinates
        FOREIGN KEY (coordinates_id)
            REFERENCES coordinates(id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_city_governor
        FOREIGN KEY (governor_id)
            REFERENCES human(id)
            ON DELETE RESTRICT
);


CREATE INDEX idx_city_name         ON city (name);
CREATE INDEX idx_city_climate      ON city (climate);
CREATE INDEX idx_city_government   ON city (government);
CREATE INDEX idx_city_coordinates  ON city (coordinates_id);
CREATE INDEX idx_city_governor     ON city (governor_id);
