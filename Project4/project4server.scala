import akka.actor._
import akka.routing.RoundRobinRouter
import scala.collection.mutable._
import scala.util._
import scala.util.control.Breaks._
import org.apache.commons.collections4.queue._

sealed trait Twitter
case class receiveTweet(flag:Int,fromUserId:Int,toUserId:Int,tweet:String,hashtag:String) extends Twitter
case class processTweet(flag:Int,fromUserId:Int,toUserId:Int,tweet:String,hashtag:String) extends Twitter
case class requestTweet(userId:Int,flag:Int,hashtag:String,requestor:ActorRef) extends Twitter
case class sendTweet(userId:Int,flag:Int,hashtag:String,requestor:ActorRef) extends Twitter
//case class GetTweets(tweets:SynchronizedQueue[String]) extends Twitter
case class GetTweets(tweets:Array[String]) extends Twitter
case class SendUsersInfo() extends Twitter
case class GetUsersInfo(totalUsers:Int, totalClients:Int, hashTags:HashMap[Int,String]) extends Twitter
case class printProcessedTweets() extends Twitter

object project4server extends App
{
	var noOfUsers = args(0).toInt
	var noOfClientMachines = args(1).toInt
	var usersTweet:HashMap[Int,TweetInfo]= new HashMap[Int,TweetInfo]()
	//var hashtagTweet:HashMap[String,SynchronizedQueue[String]]= new HashMap[String,SynchronizedQueue[String]]()
	var hashtagTweet:HashMap[String,CircularFifoQueue[String]]= new HashMap[String,CircularFifoQueue[String]]()
	//var usersInfo:HashMap[Int,ArrayBuffer[Int]]= new HashMap[Int,ArrayBuffer[Int]]()
	var totalnoOfUsers:Int = noOfUsers * noOfClientMachines 
	val queueLimit:Int = 100
	var followersRangeLimit:Int=50
	var followersRange:Int=_
	var userId:Int=_
	var a=0
	var randomFollower:Int=_
	var followersCount:Int=0 

	val system = ActorSystem("ServerTwitterSystem")
	val server = system.actorOf(Props(new Server()), name = "server")

	//Building the network
	for(a <- 0 until totalnoOfUsers)
	{
		//var followers:ArrayBuffer[Int]= new ArrayBuffer[Int]()
		userId = a
		//followersRange=Random.nextInt(followersRangeLimit)
    
		//while(followersCount != followersRange)
		//{
		//	randomFollower = Random.nextInt(totalnoOfUsers)
		//	if(userId!=randomFollower)
		//	{
		//		followers += randomFollower
		//		followersCount = followersCount +1
		//	}
		//}

		//usersInfo(userId)=followers
		usersTweet(userId) = new TweetInfo()
		//followersCount = 0
	}

	//Random generator
	val random = new Random
				 
	//Generate a random string of length n from the given characters
	def randomString(characters: String)(n: Int): String = 
	Stream.continually(random.nextInt(characters.size)).map(characters).take(n).mkString
				 
	//Generate a random string of length n (using printable characters from ASCII 33 to ASCII 126)
	def randomCharactersString(n: Int) = 
	randomString("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz")(n)
    
	hashtagTweet("#DefaultHashtag") = new CircularFifoQueue[String](20)
	//Create a HashMap of 100 hashtags
	var hashtags = new HashMap[Int,String]
	for (i <- 0 until 99)
	{
		hashtags(i) = "#" + randomCharactersString(9)
		//Initializing the hashtagTweets
		//hashtagTweet(hashtags(i)) = new SynchronizedQueue[String]()
		hashtagTweet(hashtags(i)) = new CircularFifoQueue[String](20)
	}

	class TweetInfo()
	{
		//var ownTweet:SynchronizedQueue[String]= new SynchronizedQueue[String]()
		var ownTweet:CircularFifoQueue[String]= new CircularFifoQueue[String](20)
		//var timelineTweet:SynchronizedQueue[String]= new SynchronizedQueue[String]()
		//var mentionsTweet:SynchronizedQueue[String]= new SynchronizedQueue[String]()
	}
	
	class Worker extends Actor
	{
		var sendersFollowersList:ArrayBuffer[Int]= new ArrayBuffer[Int]();
  
		def receive =
		{
			case processTweet(flag,fromUserId,toUserId,tweet,hashtag)=>
				var i:Int=0
				var j:Int=0
				//sendersFollowersList= usersInfo(fromUserId)

				flag match
				{
					//Common Followers
				  	//Process @mentions tweet that is prepended with @userid
					case 0=>
						//var receiversFollowersList:ArrayBuffer[Int]=usersInfo(toUserId)
						//Adding to Sender's Own tweets
						//if(usersTweet(fromUserId).ownTweet.size==queueLimit)
						//{
						//	usersTweet(fromUserId).ownTweet.dequeue  
						//	usersTweet(fromUserId).ownTweet+=(tweet)	       
						//}
						//else
						//{
						//	usersTweet(fromUserId).ownTweet+=(tweet)
							usersTweet(fromUserId).ownTweet.add(tweet)
						//}
						//Adding to Sender's Timeline tweets
						//if(usersTweet(fromUserId).timelineTweet.size==queueLimit)
						//{
						//	usersTweet(fromUserId).timelineTweet.dequeue  
						//	usersTweet(fromUserId).timelineTweet+=(tweet)	       
						//}
						//else
						//{
						//	usersTweet(fromUserId).timelineTweet+=(tweet)
						//}
						//Adding the tweet to receiver's mentionsTweet
						//if(usersTweet(toUserId).ownTweet.size==queueLimit)
						//{
						//	usersTweet(toUserId).ownTweet.dequeue  
						//	usersTweet(toUserId).ownTweet +=(tweet)	       
						//}
						//else
						//{
						//	usersTweet(toUserId).ownTweet +=(tweet)	
							usersTweet(toUserId).ownTweet.add(tweet)	
						//}
						//Adding the tweet to the common followers' timeline
						/*for(i<-sendersFollowersList)
						{
							for(j<-receiversFollowersList)
							{
								if(i==j)
								{
									//Adding the tweet to follower's timelineTweet
									if(usersTweet(i).timelineTweet.size==queueLimit)
									{
										usersTweet(i).timelineTweet.dequeue  
										usersTweet(i).timelineTweet+=(tweet)	       
									}  
									else
									{
										usersTweet(i).timelineTweet+=(tweet)
									}
								}
							}
						}*/

					//Normal Tweet all followers
					case 1=>
						//Adding to Sender's Own tweets
						//if(usersTweet(fromUserId).ownTweet.size==queueLimit)
						//{
						//	usersTweet(fromUserId).ownTweet.dequeue  
						//	usersTweet(fromUserId).ownTweet +=(tweet)	       
						//}
						//else
						//{
						//	usersTweet(fromUserId).ownTweet +=(tweet)
							usersTweet(fromUserId).ownTweet.add(tweet)
						//}
						//Adding to Sender's Timeline tweet
						/*if(usersTweet(fromUserId).timelineTweet.size==queueLimit)
						{
							usersTweet(fromUserId).timelineTweet.dequeue  
							usersTweet(fromUserId).timelineTweet+=(tweet)	       
						}
						else
						{
							usersTweet(fromUserId).timelineTweet+=(tweet)
						}
						var i:Int=0
						for(i<-sendersFollowersList)
						{
							//Adding the tweet to follower's timelineTweet
							if(usersTweet(i).timelineTweet.size==queueLimit)
							{
								usersTweet(i).timelineTweet.dequeue  
								usersTweet(i).timelineTweet+=(tweet)	       
							} 
							else
							{
								usersTweet(i).timelineTweet+=(tweet)
							}
						}*/
       
					//HashTag tweet
					case 2=>     
						//Adding to Sender's Own tweets
						//if(usersTweet(fromUserId).ownTweet.size==queueLimit)
						//{
						//	usersTweet(fromUserId).ownTweet.dequeue  
						//	usersTweet(fromUserId).ownTweet +=(tweet)	       
						//}
						//else
						//{
						//	usersTweet(fromUserId).ownTweet +=(tweet)
							usersTweet(fromUserId).ownTweet.add(tweet)
						//}
						//Adding to Sender's Timeline tweet
						/*if(usersTweet(fromUserId).timelineTweet.size==queueLimit)
						{
							usersTweet(fromUserId).timelineTweet.dequeue  
							usersTweet(fromUserId).timelineTweet+=(tweet)	       
						}
						else
						{
							usersTweet(fromUserId).timelineTweet+=(tweet)
						}
						var i:Int=0
						for(i<-sendersFollowersList)
						{
							//Adding the tweet to follower's timelineTweet
							if(usersTweet(i).timelineTweet.size==queueLimit)
							{
								usersTweet(i).timelineTweet.dequeue  
								usersTweet(i).timelineTweet+=(tweet)	       
							}
							else
							{
								usersTweet(i).timelineTweet+=(tweet)
							}
						}*/
						//Adding the Tweet to the hashtag queue
						//if(hashtagTweet(hashtag).size==queueLimit)
						//{
						//	hashtagTweet(hashtag).dequeue  
						//	hashtagTweet(hashtag)+=(tweet)	       
						//}	 
						//else
						//{
						//	hashtagTweet(hashtag)+=(tweet)
							try
							{
								hashtagTweet(hashtag).add(tweet)
							}
							catch
							{
								case e:Exception =>
							}	
						//}

					//Retweet: The tweet will appear in Sender's Own Tweet and/or Sender's Timeline Tweet and its followers Timeline Tweet
					case 3=>
						//var originalUsersFollowersList:ArrayBuffer[Int]=usersInfo(toUserId)
						////Adding to Sender's Own tweets
						//if(usersTweet(fromUserId).ownTweet.size==queueLimit)
						//{
						//	usersTweet(fromUserId).ownTweet.dequeue  
						//	usersTweet(fromUserId).ownTweet +=(tweet)	       
						//}
						//else
						//{
						//	usersTweet(fromUserId).ownTweet +=(tweet)
							usersTweet(fromUserId).ownTweet.add(tweet)
						//}
						/*var j:Int=0
						var following:Int=0
						breakable
						{
							for(j<-originalUsersFollowersList)
							{
								if(j==fromUserId)
								{
									   following = 1
									   break
								}
							}
						}
						//Add it to your timeline if you don't follow the original person who tweeted it
						if(following == 0)
						{
							//Adding the tweet to sender's timelineTweet
							if(usersTweet(fromUserId).timelineTweet.size==queueLimit)
							{
								usersTweet(fromUserId).timelineTweet.dequeue  
								usersTweet(fromUserId).timelineTweet +=(tweet)	       
							}
							else
							{
								usersTweet(fromUserId).timelineTweet +=(tweet)
							} 
						}
						var i:Int=0
						for(i<-sendersFollowersList)
						{
							//Adding the tweet to follower's timelineTweet
							if(usersTweet(i).timelineTweet.size==queueLimit)
							{
								usersTweet(i).timelineTweet.dequeue  
								usersTweet(i).timelineTweet+=(tweet)	       
							}
							else
							{
								usersTweet(i).timelineTweet+=(tweet)
							}
						}*/
         
					//Senders all followers
   					//Process @mentions tweet that is prepended with .@userid
   					case 4=>
						//Adding to Sender's Own tweets
						/*if(usersTweet(fromUserId).ownTweet.size==queueLimit)
						{
							usersTweet(fromUserId).ownTweet.dequeue  
							usersTweet(fromUserId).ownTweet +=(tweet)	       
						}
						else
						{
							usersTweet(fromUserId).ownTweet +=(tweet)
						}*/
						//Adding to Sender's Timeline tweet
						/*if(usersTweet(fromUserId).timelineTweet.size==queueLimit)
						{
							usersTweet(fromUserId).timelineTweet.dequeue  
							usersTweet(fromUserId).timelineTweet+=(tweet)	       
						}
						else
						{
							usersTweet(fromUserId).timelineTweet+=(tweet)
						}*/
						//Adding the tweet to receiver's mentionsTweet
						/*if(usersTweet(toUserId).ownTweet.size==queueLimit)
						{
							usersTweet(toUserId).ownTweet.dequeue  
							usersTweet(toUserId).ownTweet+=(tweet)	       
						}
						else
						{
							usersTweet(toUserId).ownTweet+=(tweet)
						}*/
						/*var i:Int=0
						for(i<-sendersFollowersList)
						{
							//Adding the tweet to follower's timelineTweet
							if(usersTweet(i).timelineTweet.size==queueLimit)
							{
								usersTweet(i).timelineTweet .dequeue  
								usersTweet(i).timelineTweet+=(tweet)	       
							}
							else
							{
								usersTweet(i).timelineTweet+=(tweet)
							}
						}*/
       		
					//Senders all followers
					//Process @mentions tweet that includes @userid in the middle
					case 5=>
						//Adding to Sender's Own tweets
						/*if(usersTweet(fromUserId).ownTweet.size==queueLimit)
						{
							usersTweet(fromUserId).ownTweet.dequeue  
							usersTweet(fromUserId).ownTweet +=(tweet)	       
						}
						else
						{
							usersTweet(fromUserId).ownTweet +=(tweet)
						}*/
						//Adding to Sender's Timeline tweet
						/*if(usersTweet(fromUserId).timelineTweet.size==queueLimit)
						{
							usersTweet(fromUserId).timelineTweet.dequeue  
							usersTweet(fromUserId).timelineTweet+=(tweet)	       
						}
						else
						{
							usersTweet(fromUserId).timelineTweet+=(tweet)
						}*/
						//Adding the tweet to receiver's mentionsTweet
						/*if(usersTweet(toUserId).ownTweet.size==queueLimit)
						{
							usersTweet(toUserId).ownTweet.dequeue  
							usersTweet(toUserId).ownTweet +=(tweet)	       
						}
						else
						{
							usersTweet(toUserId).ownTweet +=(tweet)
						}*/
						/*var i:Int=0
						for(i<-sendersFollowersList)
						{
							//Adding the tweet to follower's timelineTweet
							if(usersTweet(i).timelineTweet.size==queueLimit)
							{
								usersTweet(i).timelineTweet.dequeue  
								usersTweet(i).timelineTweet+=(tweet)	       
							}
							else
							{
								usersTweet(i).timelineTweet+=(tweet)
							}
						}*/
				}
     
			case sendTweet(userId,flag,hashtag,requestor)=>
				//var tweetQueue:SynchronizedQueue[String]= new SynchronizedQueue[String]()
				//var tweetQueue:CircularFifoQueue[String]= new CircularFifoQueue[String](20)
				var tweetQueue:Array[String] = new Array[String](20)

				//Own Tweet
				if(flag==0)
				{
					usersTweet(userId).ownTweet.toArray(tweetQueue)

				}
				else if(flag==1)
				{
					try
					{
						hashtagTweet(hashtag).toArray(tweetQueue)
					}					
					catch
					{
						case e:Exception =>
					}
					//hashtagTweet(hashtag).toArray(tweetQueue)
					
				}
				//Timeline Tweet
				/*else if(flag==2)
				{
					tweetQueue=usersTweet(userId).timelineTweet 
				}
				//Mentions Tweet
				else if(flag==3)
				{
					tweetQueue=usersTweet(userId).mentionsTweet  
				}*/
				//Hashtag Tweet
				
				requestor ! GetTweets(tweetQueue)
		}
	}

	class Server() extends Actor
	{
		val workerRouter = context.actorOf(Props[Worker].withRouter(RoundRobinRouter(12)), name = "workerRouter")
		var schedulerflag:Int=0
		var tweetRequestCount:Int=0
		var tweetResponseCount:Int=0
		var tweetRequestPerSecondCount:Int=0  
		var tweetResponsePerSecondCount:Int=0

		def receive =
		{
			//Receive tweet from client
			case receiveTweet(flag,fromUserId,toUserId,tweet,hashtag)=>
				workerRouter ! processTweet(flag,fromUserId,toUserId,tweet,hashtag)
				tweetRequestCount = tweetRequestCount + 1
				tweetRequestPerSecondCount = tweetRequestPerSecondCount + 1
				if(schedulerflag == 0)
				{
					//Schedule for printing
					import system.dispatcher
					val fd = scala.concurrent.duration.FiniteDuration(1000,"milliseconds")
					context.system.scheduler.scheduleOnce(fd, self, printProcessedTweets())
					schedulerflag = 1
				}  

			//Send tweets to client
			case requestTweet(userId,flag,hashtag,requestor)=>
				workerRouter ! sendTweet(userId,flag,hashtag,requestor)
				tweetResponseCount = tweetResponseCount + 1 
				tweetResponsePerSecondCount = tweetResponsePerSecondCount + 1
				if(schedulerflag == 0)
				{
					//Schedule for printing
					import system.dispatcher
					val fd = scala.concurrent.duration.FiniteDuration(1000,"milliseconds")
					context.system.scheduler.scheduleOnce(fd, self, printProcessedTweets())
					schedulerflag = 1
				}
				
			//Print statistics
			case printProcessedTweets()=>
				println(tweetRequestCount.toString() + "," +tweetResponseCount.toString() + ","
				+tweetRequestPerSecondCount.toString() + "," +tweetResponsePerSecondCount.toString())      
				tweetRequestPerSecondCount = 0
				tweetResponsePerSecondCount = 0
				import system.dispatcher
				val fd = scala.concurrent.duration.FiniteDuration(1000,"milliseconds")
				context.system.scheduler.scheduleOnce(fd, self, printProcessedTweets())

			//Send Users' Info to client
			case SendUsersInfo()=>
				sender ! GetUsersInfo(totalnoOfUsers,noOfClientMachines,hashtags)     
		}
	}
}
