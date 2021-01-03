# YoupHead
[NOT STABLE] WoWHead parsing for models id (TrinityCore Emulator Database) on master branche (Shadowlands).

## Concept
 1) Parse WoWHead creature page (or from an HTML dump of this page)
 2) Generate (creature_template_model, creature_model_info) in a SQL file.
 3) Creatures IDs retrieves via existing creature_template table

## Key features (educational)
 - Using common-cli to create arguments
 - Using common-io to write HTML files
 - Using jsoup to parse HTML document
 - Using mysql-connector to read MySQL table
 - Using tinylog for logging infos (info,errors) in file and console. Use this library to generate SQL queries.
