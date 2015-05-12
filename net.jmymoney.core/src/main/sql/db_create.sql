DROP TABLE IF EXISTS `transaction_splits`;
DROP TABLE IF EXISTS `transactions`;

DROP TABLE IF EXISTS `payees`;
DROP TABLE IF EXISTS `accounts`;
DROP TABLE IF EXISTS `split_partners`;

DROP TABLE IF EXISTS `categories`;
DROP TABLE IF EXISTS `user_accounts`;


CREATE TABLE `user_accounts` (
  `id` int unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `password_hash` varchar(255) NOT NULL,
  `username` varchar(255) NOT NULL,
  UNIQUE KEY (`username`)
);


CREATE TABLE `categories` (
  `id` int unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `description` longtext,
  `name` varchar(255) DEFAULT NULL,
  `parent_id` int unsigned DEFAULT NULL,
  `user_account_id` int unsigned NOT NULL,
  CONSTRAINT FOREIGN KEY (`user_account_id`) REFERENCES `user_accounts` (`id`),
  CONSTRAINT FOREIGN KEY (`parent_id`) REFERENCES `categories` (`id`)
);

CREATE TABLE `split_partners` (
  `id` int unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `description` longtext,
  `name` varchar(255) DEFAULT NULL,
  `parent_id` int unsigned DEFAULT NULL,
  `user_account_id` int unsigned NOT NULL,
  CONSTRAINT FOREIGN KEY (`user_account_id`) REFERENCES `user_accounts` (`id`),
  CONSTRAINT FOREIGN KEY (`parent_id`) REFERENCES `split_partners` (`id`)
);

CREATE TABLE `payees` (
  `id` int unsigned NOT NULL primary key,
  CONSTRAINT FOREIGN KEY (`id`) REFERENCES `split_partners` (`id`)
);

CREATE TABLE `accounts` (
  `id` int unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
  CONSTRAINT FOREIGN KEY (`id`) REFERENCES `split_partners` (`id`)
);

CREATE TABLE `transactions` (
  `id` int unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `time_stamp` datetime NOT NULL,
  `account_id` int unsigned NOT NULL,
  CONSTRAINT FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`)
);

CREATE TABLE `transaction_splits` (
  `id` int unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `amount` decimal(19,4) NOT NULL,
  `category_id` int unsigned DEFAULT NULL,
  `parent_id` int unsigned DEFAULT NULL,
  `split_partner_id` int unsigned DEFAULT NULL,
  `transaction_id` int unsigned NOT NULL,
  `note` VARCHAR(255) DEFAULT NULL, 
  CONSTRAINT FOREIGN KEY (`transaction_id`) REFERENCES `transactions` (`id`),
  CONSTRAINT FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`),
  CONSTRAINT FOREIGN KEY (`split_partner_id`) REFERENCES `split_partners` (`id`),
  CONSTRAINT FOREIGN KEY (`parent_id`) REFERENCES `transaction_splits` (`id`)
);


