The following assumptions were made:
1. The get() operation on the load balancer is more often than register(), unregister(). Thus, the balancer is optimized for get()
2. Round-robin algorithm implementation is the following:
> return pointer.getAndIncrement() % queueSize
>
Thus, in the following case of queue consisting of 5 providers:
> [0,1,2,3,4]

If provider 4 starts to flip, we will get the following providers serving the consecutive requests:
 
> pointer = 4, provider = 4
>
> provider 4 went down, pointer = 5, provider = 1
>
> provider 4 went up, pointer = 6, provider = 1
>
> provider 4 went down, pointer = 7, provider = 3

Alternative would be to store providers a linked list, 
storing pointers to the next one to serve, 
but that implementation would be less performant at the cost of "slightly more fair" round-robin.
