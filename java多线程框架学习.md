
Executor接口:
	执行者,顾名思义执行者就是执行任务的,所以他有一个方法void execute(Runnable command);执行一个任务



ExecutorService接口:
继承自Executor接口,Executor接口只有一个方法execute且返回值为空,也就是不能获取任务的执行结果,在某些情况下我们需要得到任务的执行结果.所以ExecutorService扩展了这种能力,增加了submit方法,submit方法也是执行一个任务,不同的是submit方法会返回一个Future,Future表示将来的执行结果,怎么理解呢?用submit提交了一个任务,这个任务可能需要排队,不能立即执行,在未来某个时间点才能执行,执行完之后会将结果封装在Future里面.
思考:如果提交任务与执行任务都是同一个线程,Future就没有多大意义,这时候的工作流是这样的:
  线程A提交任务->线程A执行任务->线程A得到执行结果,是一个串行化的过程
如果提交任务与执行任务不是同一个线程,Future就很有用了,举个例子:A线程提交了两个任务task1和task2分别由线程B和线程C执行,这时候的工作流程是这样的.
|A线程提交任务task1和task2并得到将来的结果Future1和Future2|
|												 													 |线程B执行task1,线程C执行task2,具体什么时间点执行由操作系统	
|												 													 |调度CPU决定,执行完成之后将结果放入Future
|线程A继续执行提交任务后的代码,不用阻塞等待线程B和线程C的 |						 	
|执行结果										  											   |
|												 													  |
|线程A询问线程B和线程C任务有没有执行完成,	Future.isDone()  |										 
 ,更骚一点的操作可以把这个Future传给另一个线程D,又线程D来轮询|
 task1和task2的执行结果,有没有框架是这么玩的呢?应该有吧,只是我|
 现在还没找到



RunnableFuture<V>接口:
	Runnable和Future<V>接口的组合,大致意思就是将任务运行的结果放到Future里面



FutureTask<V>类:
	实现了RunnableFuture<V>接口,这个值得好好研究,有执行任务的核心逻辑.
一个volatile关键字修饰的state字段,-表示任务的状态,一共定义了7种状态
一个Callable<V> callable,-具体的任务
一个Object outcome,-任务执行的结果,并没有用volatile修饰,而是通过任务状态来控制读写
一个volatile Thread runner,-执行任务的线程
一个volatile WaitNode waiters,-WaitNode是一个静态内部类,用来记录等待任务执行结果的线程,比如你领到一个很重要的开发任务,组长,经理,业务经理,老板,甲方爸爸都很关心这个任务的完成情况,很关心这个任务的执行情况,
**在来分析一下构造方法:**
	构造方法比较简单1.public FutureTask(Callable<V> callable),接收一个任务赋值给实例变量callable,并设置任务的状态为NEW.
	2.public FutureTask(Runnable runnable, V result),调用了Executors.callable(runnable, result);将runnable和result组合成一个callable赋值给实例变量,并设置任务的状态为NEW.Executors是一个工具类,你可以类比我们开发中经常写的xxxUtils工具类
**run()方法分析**
    我觉得看懂run方法至少需要两个背景知识:
	1.Unsafe类的compareAndSwapXXX方法
        Unsafe涉及到的知识比较深,需要单独介绍.这里只解释compareAndSwapXXX方法起到的作用,以及在FutureTask的run方法里为什么需要使用
        compareAndSwapXXX方法? 
	2.对Thread.currentThread()的理解
         当前线程,执行当前代码的线程,可以参考java多线程编程核心技术 第2版/高洪岩 ISBN 978-7-111-61490-6中的1.3小节

