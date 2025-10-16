DROP TABLE IF EXISTS city CASCADE;

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


CREATE TABLE city
(
    id                     BIGSERIAL PRIMARY KEY,

    -- name: NOT NULL + "не пустая строка"
    name                   VARCHAR(255) NOT NULL,
    CONSTRAINT city_name_not_blank
        CHECK (length(btrim(name)) > 0),

    -- area: положительное целое
    area                   INTEGER      NOT NULL,
    CONSTRAINT city_area_positive
        CHECK (area > 0),

    -- population: положительное целое
    population             BIGINT       NOT NULL,
    CONSTRAINT city_population_positive
        CHECK (population > 0),

    -- capital: обязательный флаг
    capital                BOOLEAN      NOT NULL,

    -- metersAboveSeaLevel:
    meters_above_sea_level INTEGER       NOT NULL,

    -- telephoneCode: обязателен, > 0
    telephone_code         INTEGER      NOT NULL,
    CONSTRAINT city_telephone_code_positive
        CHECK (telephone_code > 0),

    -- enum-поля из PG enum-типов
    climate                climate      NOT NULL,
    government             government   NOT NULL,


    -- creationDate: генерируется БД автоматически как текущая дата, NOT NULL
    creation_date          DATE         NOT NULL DEFAULT CURRENT_DATE,

    -- establishmentDate: обычная дата, может быть NULL
    establishment_date     DATE
);


CREATE INDEX idx_city_name ON city (name);
CREATE INDEX idx_city_climate ON city (climate);
CREATE INDEX idx_city_government ON city (government);
