/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package org.sywang.stu.util.concurrent;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * This class provides thread-local variables.  
 这个类提供一个线程本地变量
 These variables differ from
 * their normal counterparts in that each thread that accesses one (via its
 * {@code get} or {@code set} method) has its own, independently initialized
 * copy of the variable. 
 *线程本地变量不同于任何普遍的变量,线程本地变量仅所属线程能通过get/set方法访问
 {@code ThreadLocal} instances are typically private
 * static fields in classes that wish to associate state with a thread (e.g.,
 * a user ID or Transaction ID).
 *ThreadLocal实例是一个典型的静态私有变量,期望将状态和线程关联起来.
 *
 * <p>For example, the class below generates unique identifiers local to each
 * thread.
 *举个例子,下面的类,为每个线程生成一个唯一的id.
 * A thread's id is assigned the first time it invokes {@code ThreadId.get()}
 * and remains unchanged on subsequent calls.
 *线程的id在首次调用 ThreadId.get()的时候被分配,在随后的调用中保持不变.
 * <pre>
 *下面是这个例子的具体代码(jdk的作者也太贴心了)
 * import java.util.concurrent.atomic.AtomicInteger;
 *
 * public class ThreadId {
 *     // Atomic integer containing the next thread ID to be assigned
 *     private static final AtomicInteger nextId = new AtomicInteger(0);
 *
 *     // Thread local variable containing each thread's ID
 *     private static final ThreadLocal<Integer> threadId =
 *         new ThreadLocal<Integer>() {
 *            @Override
*			protected Integer initialValue() {
 *                 return nextId.getAndIncrement();
 *         }
 *     };
 *
 *     // Returns the current thread's unique ID, assigning it if necessary
 *     public static int get() {
 *         return threadId.get();
 *     }
 * }
 * </pre>
 * <p>Each thread holds an implicit reference to its copy of a thread-local
 * variable as long as the thread is alive and the {@code ThreadLocal}
 * instance is accessible; 
 *每个线程都持有对其线程本地副本的隐式引用只要线程处于活动状态并且 ThreadLocal 变量实例可访问
 *after a thread goes away, all of its copies of
 * thread-local instances are subject to garbage collection (unless other
 * references to these copies exist).
 *线程消失后,线程本地变量的副本将受垃圾回收的影响(除非还有其他其他引用指向这些副本)
 * @author  Josh Bloch and Doug Lea
 * @since   1.2
 */
public class ThreadLocal<T> {
    /**
     * ThreadLocals rely on per-thread linear-probe hash maps attached
     * to each thread (Thread.threadLocals and
     * inheritableThreadLocals).
	* 	ThreadLocals依赖于附加到每个线程的线性探测的has hmap.
	*The ThreadLocal objects act as keys,
     * searched via threadLocalHashCode.  
	 * 实际上ThreadLocal对象充当键,通过threadLocalHashCode搜索.
	 *This is a custom hash code
     * (useful only within ThreadLocalMaps) that eliminates collisions
     * in the common case where consecutively constructed ThreadLocals
     * are used by the same threads, while remaining well-behaved in
     * less common cases.
	 *这是一个自定义的哈希码(仅在 ThreadLocalMaps 中有用),消除冲突
	 *在连续构造 ThreadLocals 的常见情况下
     *被相同的线程使用，同时保持良好的行为
     *不太常见的情况(译者注:意思就是这是解决hash冲突使用的)
     */
    private final int threadLocalHashCode = nextHashCode();

    /**
     * The next hash code to be given out. Updated atomically. Starts at
     * zero.
     */
    private static AtomicInteger nextHashCode =
        new AtomicInteger();

    /**
	*这是一个神器的数字,至于为什么计算下一个hash值,是加上这个数字,是大有来头的.黄金分割数,斐波那契数列云云
	*https://cloud.tencent.com/developer/article/1650105
	*
     * The difference between successively generated hash codes - turns
     * implicit sequential thread-local IDs into near-optimally spread
     * multiplicative hash values for power-of-two-sized tables.
     */
    private static final int HASH_INCREMENT = 0x61c88647;

    /**
     * Returns the next hash code.
     */
    private static int nextHashCode() {
        return nextHashCode.getAndAdd(HASH_INCREMENT);
    }

    /**
     * Returns the current thread's "initial value" for this
     * thread-local variable.  
	 返回当前线程thread-local变量初始化值
	 This method will be invoked the first
     * time a thread accesses the variable with the {@link #get}
     * method, 
	 这方法会被调用,第一次用get方法访问变量.
	 unless the thread previously invoked the {@link #set}
     * method, 
	 除非这个线程提前调用了set方法.
	 in which case the {@code initialValue} method will not
     * be invoked for the thread. 
	 *在这种情况下,initialValue方法将不会被调用.
	 *Normally, this method is invoked at
     * most once per thread, 
	 *正常情况下, 这个方法每个线程只调用一次.
	 *but it may be invoked again in case of
     * subsequent invocations of {@link #remove} followed by {@link #get}.
     *但是可能会被在调用一次,get之后调用了remove.
     * <p>This implementation simply returns {@code null}; 
	 *简单实现返回null.
	 *if the programmer desires thread-local variables to have an initial
     * value other than {@code null},  {@code ThreadLocal} must be
     * subclassed, and this method overridden.
	 *如果程序员想线程本地变量有个初始值,而不是null,继承ThreadLocal并重写initialValue方法. 
	 *Typically, an anonymous inner class will be used.
     *典型,匿名内部类会使用他
     * @return the initial value for this thread-local
     */
    protected T initialValue() {
        return null;
    }

    /**
     * Creates a thread local variable. The initial value of the variable is
     * determined by invoking the {@code get} method on the {@code Supplier}.
     *
     * @param <S> the type of the thread local's value
     * @param supplier the supplier to be used to determine the initial value
     * @return a new thread local variable
     * @throws NullPointerException if the specified supplier is null
     * @since 1.8
     */
    public static <S> ThreadLocal<S> withInitial(Supplier<? extends S> supplier) {
        return new SuppliedThreadLocal<>(supplier);
    }

    /**
     * Creates a thread local variable.
     * @see #withInitial(Supplier)
     */
    public ThreadLocal() {
    }

    /**
     * Returns the value in the current thread's copy of this
     * thread-local variable.  If the variable has no value for the
     * current thread, it is first initialized to the value returned
     * by an invocation of the {@link #initialValue} method.
     *
     * @return the current thread's value of this thread-local
     */
    public T get() {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null) {
            ThreadLocalMap.Entry e = map.getEntry(this);
            if (e != null) {
                @SuppressWarnings("unchecked")
                T result = (T)e.value;
                return result;
            }
        }
        return setInitialValue();
    }

    /**
     * Variant of set() to establish initialValue. Used instead
     * of set() in case user has overridden the set() method.
     *
     * @return the initial value
     */
    private T setInitialValue() {
        T value = initialValue();
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null)
            map.set(this, value);
        else
            createMap(t, value);
        return value;
    }

    /**
     * Sets the current thread's copy of this thread-local variable
     * to the specified value.  Most subclasses will have no need to
     * override this method, relying solely on the {@link #initialValue}
     * method to set the values of thread-locals.
     *
     * @param value the value to be stored in the current thread's copy of
     *        this thread-local.
     */
    public void set(T value) {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null)
            map.set(this, value);
        else
            createMap(t, value);
    }

    /**
     * Removes the current thread's value for this thread-local
     * variable.  If this thread-local variable is subsequently
     * {@linkplain #get read} by the current thread, its value will be
     * reinitialized by invoking its {@link #initialValue} method,
     * unless its value is {@linkplain #set set} by the current thread
     * in the interim.  This may result in multiple invocations of the
     * {@code initialValue} method in the current thread.
     *
     * @since 1.5
     */
     public void remove() {
         ThreadLocalMap m = getMap(Thread.currentThread());
         if (m != null)
             m.remove(this);
     }

    /**
     * Get the map associated with a ThreadLocal. Overridden in
     * InheritableThreadLocal.
     *
     * @param  t the current thread
     * @return the map
     */
    ThreadLocalMap getMap(Thread t) {
        return t.threadLocals;
    }

    /**
     * Create the map associated with a ThreadLocal. Overridden in
     * InheritableThreadLocal.
     *
     * @param t the current thread
     * @param firstValue value for the initial entry of the map
     */
    void createMap(Thread t, T firstValue) {
        t.threadLocals = new ThreadLocalMap(this, firstValue);
    }

    /**
     * Factory method to create map of inherited thread locals.
     * Designed to be called only from Thread constructor.
     *
     * @param  parentMap the map associated with parent thread
     * @return a map containing the parent's inheritable bindings
     */
    static ThreadLocalMap createInheritedMap(ThreadLocalMap parentMap) {
        return new ThreadLocalMap(parentMap);
    }

    /**
     * Method childValue is visibly defined in subclass
     * InheritableThreadLocal, but is internally defined here for the
     * sake of providing createInheritedMap factory method without
     * needing to subclass the map class in InheritableThreadLocal.
     * This technique is preferable to the alternative of embedding
     * instanceof tests in methods.
     */
    T childValue(T parentValue) {
        throw new UnsupportedOperationException();
    }

    /**
     * An extension of ThreadLocal that obtains its initial value from
     * the specified {@code Supplier}.
     */
    static final class SuppliedThreadLocal<T> extends ThreadLocal<T> {

        private final Supplier<? extends T> supplier;

        SuppliedThreadLocal(Supplier<? extends T> supplier) {
            this.supplier = Objects.requireNonNull(supplier);
        }

        @Override
        protected T initialValue() {
            return supplier.get();
        }
    }

    /**
     * ThreadLocalMap is a customized hash map suitable only for
     * maintaining thread local values.
	 * ThreadLocalMap是一个自定义的hash map,只适合于维护线程本地变量.
	 * No operations are exported
     * outside of the ThreadLocal class. 
	 * 没有操作被导出到ThreadLocal类之外(译者:放大眼睛一看,ThreadLocalMap的属性和方法还真是私有的,另外还涉及到一个知识点,就是对静态内部类的理解).
	 * The class is package private to
     * allow declaration of fields in class Thread.  
	 * ThreadLocalMap类是包私有的,在Thread类里面允许作为一个属性.(译者:Thread与ThreadLocalMap在同一个包下)
	 * To help deal with
     * very large and long-lived usages, the hash table entries use
     * WeakReferences for keys. 
	 * 为了帮助解决非常大的,长时间存活的对象的使用,哈希表条目的key使用弱引用.
	 * However, since reference queues are not
     * used, stale entries are guaranteed to be removed only when
     * the table starts running out of space.
	 * 然而,由于引用队列没有使用弱引用, 过时的条目确保被删除,仅当哈希表超出作用域
     */
    static class ThreadLocalMap {

        /**
         * The entries in this hash map extend WeakReference, using
         * its main ref field as the key (which is always a
         * ThreadLocal object). 
		 *哈希表的条目继承弱引用,使用它主要是key作为一个ref属性
		 * Note that null keys (i.e. entry.get()
         * == null) mean that the key is no longer referenced, so the
         * entry can be expunged from table. 
		 * 注意null键意味着这个键不在被引用了,所以这个条例可以从哈希表里面删除了.
		 * Such entries are referred to
         * as "stale entries" in the code that follows.
		 * 这样的条目被认为是过时的条目,在接下来的代码中.
         */
        static class Entry extends WeakReference<ThreadLocal<?>> {
            /** The value associated with this ThreadLocal. */
            Object value;

            Entry(ThreadLocal<?> k, Object v) {
                super(k);
                value = v;
            }
        }

        /**
         * The initial capacity -- MUST be a power of two.
         */
        private static final int INITIAL_CAPACITY = 16;

        /**
         * The table, resized as necessary.
         * table.length MUST always be a power of two.
		 *哈希表的条目数组,数组长度必须为2的整数倍
         */
        private Entry[] table;

        /**
         * The number of entries in the table.
		 *哈希表的条目数
         */
        private int size = 0;

        /**
         * The next size value at which to resize.
		 *阈值,哈希表的条目数超过这个值就需要扩容了
         */
        private int threshold; // Default to 0

        /**
         * Set the resize threshold to maintain at worst a 2/3 load factor.
		 * 负载因子为2/3, 也就是说哈希表的条目数到达容量的2/3就需要扩容了. 负载因子也是hash表和讲究的一个参数,太小了浪费空间,太大了增加了哈希碰撞
		 * 的概率
         */
        private void setThreshold(int len) {
            threshold = len * 2 / 3;
        }

        /**
         * Increment i modulo len.
		 * 下一个索引的位置
         */
        private static int nextIndex(int i, int len) {
            return ((i + 1 < len) ? i + 1 : 0);
        }

        /**
         * Decrement i modulo len.
		 *上一个索引的位置
         */
        private static int prevIndex(int i, int len) {
            return ((i - 1 >= 0) ? i - 1 : len - 1);
        }

        /**
         * Construct a new map initially containing (firstKey, firstValue).
         * ThreadLocalMaps are constructed lazily, so we only create
         * one when we have at least one entry to put in it.
         */
        ThreadLocalMap(ThreadLocal<?> firstKey, Object firstValue) {
            table = new Entry[INITIAL_CAPACITY];
            //根据ThreadLocal的自定义hash码确定在数组中的位置,第一个元素肯定没有hash冲突
			int i = firstKey.threadLocalHashCode & (INITIAL_CAPACITY - 1);
            table[i] = new Entry(firstKey, firstValue);
            size = 1;
			//阈值
            setThreshold(INITIAL_CAPACITY);
        }

        /**
         * Construct a new map including all Inheritable ThreadLocals
         * from given parent map. Called only by createInheritedMap.
         *
         * @param parentMap the map associated with parent thread.
         */
        private ThreadLocalMap(ThreadLocalMap parentMap) {
            Entry[] parentTable = parentMap.table;
            int len = parentTable.length;
            setThreshold(len);
            table = new Entry[len];

            for (int j = 0; j < len; j++) {
                Entry e = parentTable[j];
                if (e != null) {
                    @SuppressWarnings("unchecked")
                    ThreadLocal<Object> key = (ThreadLocal<Object>) e.get();
                    if (key != null) {
                        Object value = key.childValue(e.value);
                        Entry c = new Entry(key, value);
                        int h = key.threadLocalHashCode & (len - 1);
                        while (table[h] != null)
                            h = nextIndex(h, len);
                        table[h] = c;
                        size++;
                    }
                }
            }
        }

        /**
         * Get the entry associated with key.  This method
         * itself handles only the fast path: a direct hit of existing
         * key. It otherwise relays to getEntryAfterMiss.  This is
         * designed to maximize performance for direct hits, in part
         * by making this method readily inlinable.
         *
         * @param  key the thread local object
         * @return the entry associated with key, or null if no such
         */
        private Entry getEntry(ThreadLocal<?> key) {
			//根据ThreadLocal的自定义hash码计算出在数组中的位置,key为空就异常了
            int i = key.threadLocalHashCode & (table.length - 1);
            Entry e = table[i];
			//有可能发送过hash碰撞,这个位置的元素并不是我们需要获取的
            if (e != null && e.get() == key)
                return e;
            else
                return getEntryAfterMiss(key, i, e);
        }

        /**
         * Version of getEntry method for use when key is not found in
         * its direct hash slot.
         *
         * @param  key the thread local object
         * @param  i the table index for key's hash code
         * @param  e the entry at table[i]
         * @return the entry associated with key, or null if no such
         */
        private Entry getEntryAfterMiss(ThreadLocal<?> key, int i, Entry e) {
            Entry[] tab = table;
            int len = tab.length;

            while (e != null) {
                ThreadLocal<?> k = e.get();
				//找到了直接返回
                if (k == key)
                    return e;
				//位置i上的元素的key为空,说明是一个过期的元素,把这个元素删除
                if (k == null)
                    expungeStaleEntry(i);
                else
					//下一个索引的位置,索引不小于数组的长度,下个索引的位置=当前索引的位置+1,否则下个索引位置从0开始
                    i = nextIndex(i, len);
				//取所在位置的元素
                e = tab[i];
            }
			
			//e为空表示在位置i上没元素,也就是key不存在
            return null;
        }

        /**
         * Set the value associated with key.
         *
         * @param key the thread local object
         * @param value the value to be set
         */
        private void set(ThreadLocal<?> key, Object value) {

            // We don't use a fast path as with get() because it is at
            // least as common to use set() to create new entries as
            // it is to replace existing ones, in which case, a fast
            // path would fail more often than not.
			//我们不使用快速路径像我们在get()方法一样.因为使用set()创建一个新条目与替换旧条目一样常见,
			//替换旧条目使用快速路径会失败

            Entry[] tab = table;
            int len = tab.length;
			//hash码对应的数组下标
            int i = key.threadLocalHashCode & (len-1);

            for (Entry e = tab[i];
				//数组下标上已经有元素了
                 e != null;
				 //
                 e = tab[i = nextIndex(i, len)]) {
				//取出元素判断一下,看看与要新添加的key是不是一样的	 
                ThreadLocal<?> k = e.get();
				//是一样的,更新一下value	
                if (k == key) {
                    e.value = value;
                    return;
                }
				//位置上有元素,但元素的key为空,说明这个元素已经过期了, (删除这个位置的元素,并把新添加的元素放到一个合适的位置)
				//这里还要考虑一种更复杂的情况:能不能直接把新添加额元素放到这个key过期的槽位上?答案是否定的.举个例子:
				//有三个元素2-v1,4-v2,6-v3,hash槽值为key/2,由于发送hash冲突,所以这三个元素在数组的位置分别为index1,index2,index3.
				//由于某种原因index2元素的key过期,但是元素还在.这是在执行 set(6,v4).6的初始位置为index1,index1还有元素且没有过期,
				//所以遍历下一个位置index2,index2元素过期了,但是不能直接把新添加的元素放在这个位置,因为key为6的元素在index3位置上已经存在了
				//那怎么解决呢?replaceStaleEntry方法就是处理这个事情的
                if (k == null) {
                    replaceStaleEntry(key, value, i);
                    return;
                }
            }
			//数组下标上没有元素,把新添加的元素放进去
            tab[i] = new Entry(key, value);
			//map大小加1
            int sz = ++size;
			//没有任何元素需要移除  且 元素个数的大小大于等于阈值
            if (!cleanSomeSlots(i, sz) && sz >= threshold)
				//rehash,扩容,移动元素的位置
                rehash();
        }

        /**
         * Remove the entry for key.
         */
        private void remove(ThreadLocal<?> key) {
            Entry[] tab = table;
            int len = tab.length;
            int i = key.threadLocalHashCode & (len-1);
            for (Entry e = tab[i];
                 e != null;
                 e = tab[i = nextIndex(i, len)]) {
                if (e.get() == key) {
                    e.clear();
                    expungeStaleEntry(i);
                    return;
                }
            }
        }

        /**
         * Replace a stale entry encountered during a set operation
         * with an entry for the specified key. 
		 * 替换一个过期的entry,在设置操作的时候使用的key
		 * The value passed in
         * the value parameter is stored in the entry, whether or not
         * an entry already exists for the specified key.
		 * entry的value使用传入的value,不管原来的value是否存在
         *
         * As a side effect, this method expunges all stale entries in the
         * "run" containing the stale entry.  (A run is a sequence of entries
         * between two null slots.)
         *
         * @param  key the key
         * @param  value the value to be associated with key
		 
         * @param  staleSlot index of the first stale entry encountered while
         *         searching for key.
		 *这个hash槽的的元素肯定是过期的
         */
        private void replaceStaleEntry(ThreadLocal<?> key, Object value,int staleSlot) {
            Entry[] tab = table;
            int len = tab.length;
            Entry e;

            // Back up to check for prior stale entry in current run.
            // We clean out whole runs at a time to avoid continual
            // incremental rehashing due to garbage collector freeing
            // up refs in bunches (i.e., whenever the collector runs).
			//向前遍历,找到有元素过期的hash槽值的开始值直到槽值为空
            int slotToExpunge = staleSlot;
            for (int i = prevIndex(staleSlot, len);
                 (e = tab[i]) != null;
                 i = prevIndex(i, len))
                if (e.get() == null)
                    slotToExpunge = i;

            // Find either the key or trailing null slot of run, whichever
            // occurs first
			//向后遍历
            for (int i = nextIndex(staleSlot, len);
				//槽值不为空
                 (e = tab[i]) != null;
                 i = nextIndex(i, len)) {
                ThreadLocal<?> k = e.get();

                // If we find key, then we need to swap it
                // with the stale entry to maintain hash table order.
                // The newly stale slot, or any other stale slot
                // encountered above it, can then be sent to expungeStaleEntry
                // to remove or rehash all of the other entries in run.
				//新添加的key以前已经添加过了
                if (k == key) {
					//显然以前存放key元素的槽位不是特别合适,因为在key以前已经出现了过期的槽位
					//所有把新添加的元素放到已过期的槽位上, i槽位放已过期的元素
                    e.value = value;
                    tab[i] = tab[staleSlot];cleanSomeSlots(expu
                    tab[staleSlot] = e;
					
                    // Start expunge at preceding stale entry if it exists
					//staleSlot槽位之前没有过期的槽位,开始过期的槽位就可以调整为当前的槽位i了
                    if (slotToExpunge == staleSlot)
                        slotToExpunge = i;
					//首先清除连续肯定过期的的槽位的值(做一个连续段的清理,返回元素为空的槽位[肯定没有过期]),
					//在做一个启发式的清理
                    ngeStaleEntry(slotToExpunge), len);
                    return;
                }

                // If we didn't find stale entry on backward scan, the
                // first stale entry seen while scanning for key is the
                // first still present in the run.
				//槽值过期了,且staleSlot之前没有过期的曹值.把过期槽值推进到当前位置
                if (k == null && slotToExpunge == staleSlot)
                    slotToExpunge = i;
            }

            // If key not found, put new entry in stale slot
			//找了一圈这个key以前没添加过,把新添加的元素放到staleSlot位置
            tab[staleSlot].value = null;
            tab[staleSlot] = new Entry(key, value);

            // If there are any other stale entries in run, expunge them
			//如果slotToExpunge==staleSlot说明比较紧凑,staleSlot前后都没有过期的元素(包括staleSlot已被新添加的元素顶替),就不需要做清理动作
            if (slotToExpunge != staleSlot)
                cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);
        }

        /**
         * Expunge a stale entry by rehashing any possibly colliding entries
         * lying between staleSlot and the next null slot.  This also expunges
         * any other stale entries encountered before the trailing null.  See
         * Knuth, Section 6.4
         *
         * @param staleSlot index of slot known to have null key
         * @return the index of the next null slot after staleSlot
         * (all between staleSlot and this slot will have been checked
         * for expunging).
         */
		 /**
		 *清除过期的entry,输入参数的hash槽位上的元素肯定是过期的,需要清除.
		 *还依次判断下一个槽位的entry有没有过期,直到下一个槽位的entry为空.
		 *下一个槽位的元素过期了直接删除,下一个槽位的元素没有过期,重新给他找个合适的位置
		 */
        private int expungeStaleEntry(int staleSlot) {
            Entry[] tab = table;
            int len = tab.length;

            // expunge entry at staleSlot
            tab[staleSlot].value = null;
            tab[staleSlot] = null;
            size--;

            // Rehash until we encounter(遇到) null
            Entry e;
            int i;
			//直到hash槽位的entry为空
            for (i = nextIndex(staleSlot, len);
                 (e = tab[i]) != null;
                 i = nextIndex(i, len)) {	
                ThreadLocal<?> k = e.get();
				//entry的key为空,是一个过期的entry,直接清除
                if (k == null) {
                    e.value = null;
                    tab[i] = null;
                    size--;
				//entry的key不为空
                } else {
					//entry的key的hash值
                    int h = k.threadLocalHashCode & (len - 1);
					//entry的key的hash值 与 当前放entry的槽位不匹配,  给entry挪挪位置, 把entry放到可能更合适的位置
                    if (h != i) {
                        tab[i] = null;
						//Knuth高德纳的著作TAOCP（《计算机程序设计艺术》）的6.4章节（散列）R算法
                        // Unlike Knuth 6.4 Algorithm R, we must scan until
                        // null because multiple entries could have been stale.
						//从最匹配的位置开始匹配,如果最匹配的位置已经有元素了,就取下一个位置
                        while (tab[h] != null)
                            h = nextIndex(h, len);
                        tab[h] = e;
                    }
					//...
                }
            }
            return i;
        }

        /**
         * Heuristically scan some cells looking for stale entries.
		 * 启发式的扫描一些槽位为了查找一些过期的entry
         * This is invoked when either a new element is added, or
         * another stale one has been expunged.
	     * 这个方法被调用,当一个新元素被添加的时候,或者其他过期元素被删除的时候
		 * It performs a logarithmic number of scans, 
		 * 提供对数的扫描次数
		 * as a balance between no
         * scanning (fast but retains garbage) and a number of scans
         * proportional to number of elements, that would find all
         * garbage but would cause some insertions to take O(n) time.
		 * 是一种均衡,不扫描(快,但是会保留过期元素) 和 扫描所有元素(能清除所有过期元素,但是可能会导致部分插入花费O(n)时间)之间的均衡.
         *
         * @param i a position known NOT to hold a stale entry. The
         * scan starts at the element after i.
		 * i位置的元素肯定没有过期
         *
         * @param n scan control: {@code log2(n)} cells are scanned,
         * unless a stale entry is found, in which case
         * {@code log2(table.length)-1} additional cells are scanned.
		 * log2(n)元素会被扫描到,除非找到过期的元素,这种请情况下额外的log2(table.length)-1元素会被扫描到
         * When called from insertions, this parameter is the number
         * of elements, 
		 * 插入时调用的该方法,传递的参数是map元素的个数
		 * but when from replaceStaleEntry, it is the
         * table length. 
		 * 替换过期元素调用该方法,传递的参数是map的容量
		 * (Note: all this could be changed to be either
         * more or less aggressive by weighting n instead of just
         * using straight log n. But this version is simple, fast, and
         * seems to work well.)
		 * (这段的大意是(译者):直接取n的对数是有点暴力的,考虑使用n的权重.但是这个版本就用n的对数简单快速的实现一下,这看起来也工作得很好)
         *
         * @return true if any stale entries have been removed.
		 * 
         */
        private boolean cleanSomeSlots(int i, int n) {
            boolean removed = false;
            Entry[] tab = table;
            int len = tab.length;
            do {
				//因为i没有过期,从i向后查询
                i = nextIndex(i, len);
                Entry e = tab[i];
                if (e != null && e.get() == null) {
                    n = len;
                    removed = true;
                    i = expungeStaleEntry(i);
                }
            } while ( (n >>>= 1) != 0);
            return removed;
        }

        /**
         * Re-pack and/or re-size the table. First scan the entire
         * table removing stale entries. If this doesn't sufficiently
         * shrink the size of the table, double the table size.
         */
        private void rehash() {
            expungeStaleEntries();

            // Use lower threshold for doubling to avoid hysteresis
            if (size >= threshold - threshold / 4)
                resize();
        }

        /**
         * Double the capacity of the table.
         */
        private void resize() {
            Entry[] oldTab = table;
            int oldLen = oldTab.length;
            int newLen = oldLen * 2;
            Entry[] newTab = new Entry[newLen];
            int count = 0;

            for (int j = 0; j < oldLen; ++j) {
                Entry e = oldTab[j];
                if (e != null) {
                    ThreadLocal<?> k = e.get();
                    if (k == null) {
                        e.value = null; // Help the GC
                    } else {
                        int h = k.threadLocalHashCode & (newLen - 1);
                        while (newTab[h] != null)
                            h = nextIndex(h, newLen);
                        newTab[h] = e;
                        count++;
                    }
                }
            }

            setThreshold(newLen);
            size = count;
            table = newTab;
        }

        /**
         * Expunge all stale entries in the table.
         */
        private void expungeStaleEntries() {
            Entry[] tab = table;
            int len = tab.length;
            for (int j = 0; j < len; j++) {
                Entry e = tab[j];
                if (e != null && e.get() == null)
                    expungeStaleEntry(j);
            }
        }
    }
}
