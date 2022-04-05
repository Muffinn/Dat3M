#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <pthread.h>
#include <lkmm.h>
#include <assert.h>

// spinlock.h
//
struct spinlock_s {
    atomic_t lock;
};

typedef struct spinlock_s lkmm_spinlock_t;

static inline void spinlock_init(struct spinlock_s *l);
static inline void spinlock_acquire(struct spinlock_s *l);
static inline int spinlock_tryacquire(struct spinlock_s *l);
static inline void spinlock_release(struct spinlock_s *l);

// spinlock.c
//
static inline void spinlock_init(struct spinlock_s *l)
{
    atomic_set(&l->lock, 0);
}

static inline void await_for_lock(struct spinlock_s *l)
{
    while (atomic_read(&l->lock) != 0)
        ;
    return;
}

static inline int try_get_lock(struct spinlock_s *l)
{
    int val = 0;
    return atomic_cmpxchg_acquire(&l->lock, val, 1);
}

static inline void spinlock_acquire(struct spinlock_s *l)
{
    do {
        await_for_lock(l);
    } while(!try_get_lock(l));
    return;
}

static inline int spinlock_tryacquire(struct spinlock_s *l)
{
    return try_get_lock(l);
}

static inline void spinlock_release(struct spinlock_s *l)
{
    atomic_set_release(&l->lock, 0);
}

// main.c
//
int shared;
lkmm_spinlock_t lock;
int sum = 0;

void *thread_n(void *arg)
{
    intptr_t index = ((intptr_t) arg);

    spinlock_acquire(&lock);
    shared = index;
    int r = shared;
    assert(r == index);
    sum++;
    spinlock_release(&lock);
    return NULL;
}

// variant
//
int main()
{
    pthread_t t0, t1, t2, t3, t4;

    spinlock_init(&lock);
    pthread_create(&t0, NULL, thread_n, (void *) 0);
    pthread_create(&t1, NULL, thread_n, (void *) 1);
    pthread_create(&t2, NULL, thread_n, (void *) 2);
    pthread_create(&t3, NULL, thread_n, (void *) 3);
    pthread_create(&t4, NULL, thread_n, (void *) 4);

    pthread_join(t0, 0);
    pthread_join(t1, 0);
    pthread_join(t2, 0);
    pthread_join(t3, 0);
    pthread_join(t4, 0);
    
    assert(sum == 5);
    
    return 0;
}
