# This is a project to evolve a clojure structure for implementing systems designed using Event Modeling

Event Modeling is a great way to model systems, focusing on business value. When you factor in 'residuality theory' it means that we need to architect and develop in such a way that we can layer in things like Authn, Authz, ReBAC, infra, retries, logging etc. 

We develop in slices to match capabilities. If a slice isn't needed or needs to be redone we can throw it away and the system isn't broken.

