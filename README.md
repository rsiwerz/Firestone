# Skitbra Referenser
* [Aphyr - Core language concepts](https://aphyr.com/posts/266-core-language-concepts)
* [Learn Clojure](https://clojure.org/guides/learn/syntax)
* [Weird Characters](https://clojure.org/guides/weird_characters)

# Skitkul Bloggposter
* [Reversing the technical interview](https://aphyr.com/posts/340-reversing-the-technical-interview)
* [Hexing the technical interview](https://aphyr.com/posts/341-hexing-the-technical-interview)
* [Typing the technical interview (Haskell)](https://aphyr.com/posts/342-typing-the-technical-interview)


# Funktioner

```clojure
(apply f x1 x2 ... xn args)
```

Utvärderar funktionen f med argumenten `x1..xn ++ args`

## Lambda-funktion

[%, %n, %& - Anonymous function arguments](https://clojure.org/guides/weird_characters#__code_code_code_n_code_code_code_anonymous_function_arguments)

```clojure
; anonymous function taking a single argument and printing it
(fn [line] (println line))

; anonymous function taking a single argument and printing it - shorthand
#(println %)
```

```clojure
(===
  (+ %1 %2)
  (fn [a1 a2] + a1 a2))
```

```clojure
(#(+ 1 % %) 2 3)
ArityException Wrong number of args (2) passed to: string/eval4964/fn--4965  clojure.lang.AFn.throwArity (AFn.java:429)
(#(+ 1 %) 2)
=> 3
```

## Currying (`partial`)
```clojure
((partial f a b c) x y z)
```
evaluerar till:
```clojure
(f a b c x y z)
```




# Atoms

[Clojure/Atoms](https://clojure.org/reference/atoms) 

En Atom är som en variabel. Minne som kan muteras

## `(atom x)`
Skapar en atom med värdet x.

Exempel:
```clojure
(atom {})
```

## `deref`

Plocka ut värdet från en ref/agent/var/atom/delay/future/promise.

## `swap!`

```clojure
(swap! a f x1 x2 ... xn)
```

Muterar a till att bli

```clojure
(f (deref a) x1 x2 ... xn)
```

returnerar nya `(deref a)`

## `defonce`

```clojure
(defonce definitions-atom (atom {}))
```

Om definitions-atom inte redan är definerad:
```clojure
(def definitions-atom (atom {}))
```

## Refs vs Atoms vs Agents vs Vars
Från [Stack Overflow](https://stackoverflow.com/questions/9132346/clojure-differences-between-ref-var-agent-atom-with-examples):

I highly recommend "The Joy of Clojure" or "programming Clojure" for a real answer to this question, I can reproduce a short snip-it of the motivations for each:

start by watching this video on the notion of Identity and/or studying here.

* Refs are for Coordinated Synchronous access to "Many Identities".
* Atoms are for Uncoordinated synchronous access to a single Identity.
* Agents are for Uncoordinated asynchronous access to a single Identity.
* Vars are for thread local isolated identities with a shared default value.

Coordinated access is used when two Identities needs to change together, the classic example is moving money from one bank account to another, it needs to either move completely or not at all.

Uncoordinated access is used when only one Identity needs to update, this is a very common case.

Synchronous access is used when the call is expected to wait until all Identities have settled before continuing.

Asynchronous access is "fire and forget" and let the Identity reach its new state in its own time.
