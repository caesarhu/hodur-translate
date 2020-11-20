CREATE TABLE items
(carry TEXT NOT NULL ,
 check_sign TEXT NOT NULL ,
 check_time TIMESTAMP NOT NULL ,
 file TEXT NOT NULL ,
 file_time TIMESTAMP NOT NULL ,
 flight TEXT ,
 id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY ,
 ip TEXT ,
 memo TEXT ,
 passenger_sign TEXT ,
 police TEXT NOT NULL ,
 process TEXT NOT NULL ,
 subunit TEXT ,
 trader_sign TEXT ,
 unit TEXT NOT NULL)
;
