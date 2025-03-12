# Majeurs

- [x] config pour ne pas créer le shema automatiquement
- [ ] attention à la taille de cette variable : rowErrors (ram)
- [x] inject des variables d'env dans le .yml de test
- [ ] parsing doctrine mysql database url en url jdbc et username+password : snipet shell
- [ ] lire plutot un .property (au lieu d'un .env) : parsing native, et usage de var d'env
- [ ] commit useCasesConfig avec un readme de lancement, un .sh de lancement, un .property.example, un application.yml


# Refacto
- [ ] deplace le bloc config retry dans api
- [ ] suuprimer le code inutilisés
- [ ] refactos iterratifs continues

# Avancée
- [ ] option sql loop jusqu'a ce que le sql ne retourne aucune ligne 
- [ ] multi threading sur les call d'api
- [ ] avan execution prejob: analyse sql, analyse freemarquer, routes, verbe...

# Qualités

- [ ] créer de fichiers .yml par profile (auth-demo, etc) afin de pouvoir demontrer le fonction d'un profile independamment des autres
- [ ] test template file reader