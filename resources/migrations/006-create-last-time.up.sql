CREATE TABLE last_time
(fail BIGINT NOT NULL ,
 file_time TIMESTAMP NOT NULL ,
 id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY ,
 success BIGINT NOT NULL ,
 total BIGINT NOT NULL)
;
--;;
CREATE INDEX last_time_file_time_index ON last_time (file_time) ;
