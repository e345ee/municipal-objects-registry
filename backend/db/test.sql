BEGIN;


DELETE FROM city;
DELETE FROM human;
DELETE FROM coordinates;


INSERT INTO coordinates (x, y) VALUES (100.0, 50.0);
INSERT INTO coordinates (x, y) VALUES (200.0, 20.0);
INSERT INTO coordinates (x, y) VALUES (300.0, 10.0);


INSERT INTO human (height) VALUES (180.5);
INSERT INTO human (height) VALUES (165.0);
INSERT INTO human (height) VALUES (190.0);




INSERT INTO city (
    name, area, population, capital, meters_above_sea_level,
    telephone_code, climate, government, establishment_date,
    coordinates_id, governor_id
) VALUES (
             'Alpha City',
             300,
             500000,
             TRUE,
             20,
             111,
             'RAIN_FOREST',
             'DEMARCHY',
             DATE '1800-05-01',
             1,
             1
         );


INSERT INTO city (
    name, area, population, capital,
    telephone_code, climate, government,
    coordinates_id
) VALUES (
             'Beta Point',
             100,
             50000,
             FALSE,
             222,
             'TUNDRA',
             'KLEPTOCRACY',
             1
         );


INSERT INTO city (
    name, area, population, capital,
    meters_above_sea_level, telephone_code,
    climate, government, establishment_date,
    coordinates_id, governor_id
) VALUES (
             'Gamma Harbor',
             250,
             200000,
             FALSE,
             10,
             495,
             'HUMIDSUBTROPICAL',
             'PLUTOCRACY',
             DATE '1950-11-02',
             2,
             2
         );


INSERT INTO city (
    name, area, population, capital,
    telephone_code, climate,
    coordinates_id, governor_id
) VALUES (
             'Delta Village',
             80,
             20000,
             FALSE,
             333,
             'RAIN_FOREST',
             3,
             3
         );


INSERT INTO city (
    name, area, population, capital,
    climate, coordinates_id, telephone_code
) VALUES (
             'Echo Town',
             120,
             55000,
             TRUE,
             'TUNDRA',
             2,
          343
         );

COMMIT;



SELECT * FROM coordinates;
SELECT * FROM human;
SELECT * FROM city ORDER BY id;
