# Chapter 2: Event Sourcing - what is it?

The Events in the Event Store are the single source of truth for the system. 

We can build any order-related projection out of these events.

By using Event Sourcing we align our systems better to the real world (such as how the business actually works), rather than creating a system based on the techie world and feeling frustrated forever after.

We tend to want to jump into implementing the system and talking about technology before understanding the business problems that need to be solved.

When we thing about Events we start to think about what the system actually does, so more about behaviour.

Since an event is something that happened it cannot be undone, this means events as data must be immutable.

In an Event Store there are event streams that represent the chronological sequence of all state-changing events that have occurred for business entity or process. Each event is a discrete change and is the result of some triggered action.

Business entities in an Event Store are typically identified by their Stream-Id.

We can replay the events to get the latest state of the Business Entity, or we can decide to see the state from a particular date by only replaying the events up to that date.

This provides great audit capabilities.

We can use the events in the event streams to create projections for different use cases. With Event Sourcing we can build projections today for use cases we didn't have in the past with all the data we gather in the events.

An event store has append only event streams, the streams don't change and the events in them don't change. All that we do is add to the event streams.

Event Stores provide efficient access to the event streams (event sequences). We could be accessing all of the events for a customer or for an entity, this could be limited to a certain timeframe.


