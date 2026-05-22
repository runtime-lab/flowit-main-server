CREATE DATABASE IF NOT EXISTS project_flowit
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'flowit_dev'@'%'
    IDENTIFIED BY 'flowitDevPass';

GRANT ALL PRIVILEGES ON project_flowit.* TO 'flowit_dev'@'%';

FLUSH PRIVILEGES;