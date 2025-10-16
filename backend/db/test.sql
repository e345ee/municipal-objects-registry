-- =========================
-- DML demo: тестовая вставка
-- =========================

-- Обрати внимание: creation_date мы НЕ задаём — его поставит БД (DEFAULT CURRENT_DATE)
INSERT INTO city (
    name,
    area,
    population,
    capital,
    meters_above_sea_level,
    telephone_code,
    climate,
    government,
    establishment_date
) VALUES (
             'Sample City',
             250,                  -- area > 0
             1200000,              -- population > 0
             TRUE,                 -- capital NOT NULL
             180,                  -- metersAboveSeaLevel (может быть NULL, здесь для примера 180)
             812,                  -- telephoneCode > 0
             'HUMIDSUBTROPICAL',   -- одно из: RAIN_FOREST | HUMIDSUBTROPICAL | TUNDRA
             'DEMARCHY',           -- одно из: DEMARCHY | KLEPTOCRACY | CORPORATOCRACY | PLUTOCRACY | THALASSOCRACY
             DATE '1890-05-20'     -- establishment_date (может быть NULL)
         );


SELECT * FROM city;
