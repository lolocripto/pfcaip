# ¿Qué cambios son viables en Lucene? #

## Tener informacion estadistica por Documento ##
La informacion en sí se puede sacar ya que el procesamiento se efectua por documento y luego por "Termino" (siendo cada Termino el par "Campo/Termino"), los problemas que plantea son:
  * Mantener en memoria la informacion de cada Termino por Documento distinto, debería ser alguna estructura lo mas rapida posible para no penalizar demasiado en los accesos cuando haya gran cantidad de datos a indexar
  * Escribir dicha informacion, para aprovechar la ventaja de la rapidez de Lucene se debería hacer mediante el mismo procedimiento y esto, al menos hoy por hoy, me resulta muy dificil
  * Leer los datos: esta el problema de leer esa gran cantidad de datos sin penalizar de nuevo en tiempo, además habría que dejarlo disponible en suficientes objetos para que resulte útil a la hora de acceder a ellos

## Crear un campo nuevo global ##
> Solo se podria aglutinar informacion global de la collection no? no podriamos obtener informacion estadistica por documento.

## Crear un campo nuevo por documento ##
> Aqui tenemos el problema del acceso a los parametros de cada documento, cómo seleccionamos el documento? a la hora de guardar los campos usariamos el "id" del documento, pero como bien dice la documentacion, hay que tener mucho cuidado de usar este valor puesto que va cambiando con las operaciones de merging, optimizing, etc ... y sólo garantizan además que es único por segmento.
> No veo muy viable esta solución ...

## Tener contadores globales por Termino manteniendo las estructuras actuales ##
> Llevar cuenta de los Terminos que van saliendo (Terminos finales, sin depender del Campo) manteniendolos en una tabla Hash e ir contando el numero de Terminos diferentes, entonces se podría incluir dicha informacion en el objeto TermInfo, si bien este objeto se crea por cada Campo/Termino, yo podría poner la misma informacion del termino (en cuanto a su contador global) en todos los campos en los que aparezca, por ejemplo:
doc\_0:
> > content1: aaa
> > content1: bbb
> > content2: aaa
doc\_1:
> > content1: aaa
> > content1: bbb
> > content2: aaa

aqui habria un total de 3 objetos TermInfo:

> content1 / aaa --> con valores para DF (doc frequency) y CF (collection frequency) iguales, 2
> content1 / bbb --> con valores para DF y CF iguales, 2
> content2 / aaa --> con valores para DF y CF iguales, 2
los valores DF y el CF coinciden porque el termino "aaa" esta en dos campos distintos, pero si mantenemos una tabla hash con los terminos y su contador global se pondria un nuevo parametro CFG (collection frequency global) en los 3 TermInfo anteriores:
> content1 / aaa --> con valores para DF y CF iguales, 2 y CFG 4
> content1 / bbb --> con valores para DF y CF iguales, 2 y CFG 2
> content2 / aaa --> con valores para DF y CF iguales, 2 y CFG 4

Problemas:
> - No tenemos informacion de Termino por Documento y BM25 la necesita
> - Ocupamos mas memoria a la hora de crear el indice, una gran tabla Hash con todos los terminos: ya tenemos una tabla hash con los terminos por campo
> - Tiempo de procesamiento a la hora de crear el indice: por cada termino necesitamos hacer la comprobacion de si el termino esta en la tabla hash o no, eso consume tiempo

Ventajas:
> - Tendriamos el numero de veces que aparece un termino (sin importar el campo en el que esté) en toda la colección.
> - Estaría disponible con el resto de la informacion del Término y se guardaria como parte del diccionario existente ahora (`*`.tis) asi que evitariamos el problema de almacenar en un fichero nuevo la informacion

> Se esta manteniendo una conversación interesante acerca de todo esto con los usuarios de Lucene: https://issues.apache.org/jira/browse/LUCENE-2091