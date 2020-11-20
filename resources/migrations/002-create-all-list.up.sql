CREATE TABLE all_list
(id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY ,
 item TEXT NOT NULL ,
 items_id BIGINT NOT NULL REFERENCES items (id) ON UPDATE CASCADE ON UPDATE CASCADE ,
 quantity BIGINT NOT NULL)
;
--;;
CREATE INDEX all_list_item_index ON all_list (item) ;
--;;
CREATE INDEX all_list_items_id_index ON all_list (items_id) ;
