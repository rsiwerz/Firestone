# Simple Queue™

A queue is a list, a vector or a map with the keys `:front` and `:back`.
```clojure
(queue (list 1 2 3) [4 5 6])
=> {:front (1 2 3), :back [4 5 6]}
```
The front is then a list and the back a vector. Items are
enqueued to `:back` and dequeued from `:front`.
When `:front` is empty, `:back` is converted to a list.

## Functions
`qpeek` returns the first item.
```clojure
(-> (queue (list 1 2 3) [4 5 6])
    (qpeek))
=> 1
```
`dequeue` removes the first item from the queue.
```clojure
(-> (queue (list 1 2 3) [4 5 6])
    (dequeue))
=> {:front (2 3), :back [4 5 6]}
```
`enqueue` puts an item in the end of the queue.
```clojure
(-> (queue (list 1 2 3) [4 5 6])
    (enqueue 7))
=> {:front (1 2 3), :back [4 5 6 7]}
```
`enqueue-event` puts an event in the event-queue in the state
```clojure
(-> (create-game)
    (enqueue-event {:event-type :draw-card
                    :player-id  "p1"}))
=>
{:player-id-in-turn "p1",
 :players {...},
 :counter 1,
 :minion-ids-summoned-this-turn [],
 :events [{:event-type :draw-card, :player-id "p1"}]}
```
`qflatten` converts its argument to a list.
```clojure
(-> (queue (list 1 2 3) [4 5 6])
    (qflatten))
=> (1 2 3 4 5 6)
```