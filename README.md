# jawampa-android

Based on https://github.com/Matthias247/jawampa

This library has 2 major changes:
 - Use Gson instead of jackson.
 - Use [RxAndroid](https://github.com/ReactiveX/RxAndroid)

### Why use Gson instead of jackson ?

Essentially because jackson has 10 times more methods than Gson and also because for my project i send a lot of small message and according [this post] 
(http://blog.takipi.com/the-ultimate-json-library-json-simple-vs-gson-vs-jackson-vs-json):

> If you have an environment that deals often or primarily with big JSON files, then Jackson is your library of interest. GSON struggles the most with big files.

---

> If your environment primarily deals with lots of small JSON requests, such as in a micro services or distributed architecture setup, then GSON is your library of interest. Jackson struggles the most with small files.
