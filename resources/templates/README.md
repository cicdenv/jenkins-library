## Purpose
Files "rendered" with Groovy Temlpate Engines.

```groovy
// SimpleTemplateEngine
def message = '''\
Hello "$firstname $lastname",

So nice to meet you in <% print city %>.
See you in ${month},

${signed}
'''

def bindings = [
  "firstname" : "Random", 
  "lastname" : "Person",
  "city" : "San Francisco", 
  "month" : "December", 
  "signed" : "Jenkins Agent",
]

def engine = new groovy.text.SimpleTemplateEngine()
def template = engine.createTemplate(message).make(bindings)

def rendered = template.toString()
```

## Links
* http://docs.groovy-lang.org/docs/next/html/documentation/template-engines.html
* https://groovyconsole.appspot.com/
