-- Create a dedicated database for each proposed microservice

CREATE DATABASE catalog_db;
CREATE DATABASE dataset_db;
CREATE DATABASE build_db;
CREATE DATABASE scheduling_db;
CREATE DATABASE code_db;
CREATE DATABASE authpolicy_db;
CREATE DATABASE notification_db;

-- We don't need to create separate schemas right now because Spring Boot/Flyway 
-- will connect to each individual database and initialize the public schema there.
