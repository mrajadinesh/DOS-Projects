import akka.actor._
import akka.routing.RoundRobinRouter
import scala.util._
import scala.collection.mutable._
import org.apache.commons.collections4.queue._

sealed trait Twitter
case class GetUsersInfoFromServer() extends Twitter
case class SendUsersInfo() extends Twitter
case class GetUsersInfo(totalUsers:Int, totalClients:Int, hashTags:HashMap[Int,String]) extends Twitter
case class Start() extends Twitter
case class StartTweeting() extends Twitter
case class StartGetting() extends Twitter
case class StartPeakGeneration() extends Twitter
case class receiveTweet(flag:Int,fromUserId:Int,toUserId:Int,tweet:String,hashtag:String) extends Twitter
case class requestTweet(userId:Int,flag:Int,hashtag:String,requestor:ActorRef) extends Twitter
case class GetTweets(tweets:Array[String]) extends Twitter

object project4client extends App 
{
  val serverIp = args(0)
  val clientNo = args(1)
  val system = ActorSystem("ClientTwitterSystem")
  val clientMaster = system.actorOf(Props(new Master()), name = "master")
  clientMaster ! GetUsersInfoFromServer()
  
  //Global Variables
  var nrOfUsers: Int = _
  var nrOfClients: Int = _
  var hashtags: HashMap[Int,String] = _
  var serverRef: ActorRef = _
  var nrOfUsersPerClient:Int = _
  var startIndex:Int = _
  var endIndex:Int = _
  var usersInfo:HashMap[Int,ArrayBuffer[Int]]= new HashMap[Int,ArrayBuffer[Int]]()
  var followersRangeLimit:Int=50
  var followersRange:Int=_
  var userId:Int=_
  var a=0
  var randomFollower:Int=_
  var followersCount:Int=0 
  
  //Random generator
  val random = new Random
				 
  //Generate a random string of length n from the given characters
  def randomString(characters: String)(n: Int): String = 
  Stream.continually(random.nextInt(characters.size)).map(characters).take(n).mkString
				 
  //Generate a random string of length n (using printable characters from ASCII 33 to ASCII 126)
  def randomCharactersString(n: Int) = 
  randomString("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz")(n)
  
  class Master() extends Actor 
  {
    //Create actors
    var userRouter: ActorRef = context.actorOf(Props[User].withRouter(RoundRobinRouter(12)), name = "userRouter")
    val connectionString = "akka.tcp://ServerTwitterSystem@" + serverIp + ":4570/user/server"
    serverRef = context.actorFor(connectionString)
  
    def receive = 
    {
      case Start() =>
        println("Started tweeting and getting...")
        for (i <- 1 to 4)
        {
          userRouter ! StartTweeting()
          userRouter ! StartGetting()
          userRouter ! StartPeakGeneration()
        }
      
      case GetUsersInfo(totalUsers, totalClients, hashTags) =>
        println("Got Users' Info...")
        nrOfUsers = totalUsers
        nrOfClients = totalClients
        hashtags = hashTags
        nrOfUsersPerClient = nrOfUsers/nrOfClients
        startIndex = (clientNo.toInt - 1)*nrOfUsersPerClient
        endIndex = clientNo.toInt*nrOfUsersPerClient - 1

        //Building the network
	for(a <- startIndex to endIndex)
	{
		var followers:ArrayBuffer[Int]= new ArrayBuffer[Int]()
		userId = a
		followersRange=Random.nextInt(followersRangeLimit)
    
		while(followersCount != followersRange)
		{
			randomFollower = Random.nextInt(totalUsers)
			if(userId!=randomFollower)
			{
				followers += randomFollower
				followersCount = followersCount +1
			}
		}

		usersInfo(userId)=followers
		followersCount = 0
	}
        self ! Start()
      
      case GetUsersInfoFromServer() =>
        println("Getting Users' Info...")
        serverRef ! SendUsersInfo()
    }
  }
  
  class User extends Actor 
  {
    def receive = 
    {
      case StartTweeting() =>
          for (i <- 1 to 75)
          {
            var fromUserId:Int = Random.nextInt(endIndex - startIndex + 1) + startIndex;
            var flag:Int = Random.nextInt(3)
            if(flag == 0)
            {
              //Send @mentions tweet prepending with @userid
        
              //Get a random userid for @mentions
              var followers:ArrayBuffer[Int] = usersInfo(fromUserId)
              if(followers.size > 0)
              {
                var followerIndex:Int = Random.nextInt(followers.size)
                var toUserId = followers(followerIndex)
                //var toUserId:Int = Random.nextInt(endIndex - startIndex + 1) + startIndex;
                var tweet:String = "@" + toUserId.toString() + " " + randomCharactersString(10)
                serverRef ! receiveTweet(0, fromUserId, toUserId, tweet, "")
              }
            }
            else if(flag == 1)
            {
              //Send normal tweet
              var tweet:String = randomCharactersString(10)
              serverRef ! receiveTweet(1, fromUserId, -1, tweet, "")
            }
            else if(flag == 2)
            {
              //Send hashtagged tweet
        
              //Get a random hashtag from the existing hashtags
              var randomHashtagId:Int = Random.nextInt(100)
              val hashtag:String = hashtags.getOrElse(randomHashtagId, "#DefaultHashtag")
              var tweet:String = randomCharactersString(5) + " " + hashtag + " " + randomCharactersString(5)
              serverRef ! receiveTweet(2, fromUserId, -1, tweet, hashtag)
            }
            /*else if(flag == 4)
            {
              //Send @mentions tweet prepending with .@userid
        
              //Get a random userid for @mentions
              var toUserId:Int = Random.nextInt(endIndex - startIndex + 1) + startIndex;
              var tweet:String = ".@" + toUserId.toString() + " " + randomCharactersString(10)
              serverRef ! receiveTweet(flag, fromUserId, toUserId, tweet, "")
            }
            else if(flag == 5)
            {
              //Send @mentions tweet including @userid in the middle
        
              //Get a random userid for @mentions
              var toUserId:Int = Random.nextInt(endIndex - startIndex + 1) + startIndex;
              var tweet:String = randomCharactersString(5) + " " + toUserId.toString() + " " + randomCharactersString(5)
              serverRef ! receiveTweet(flag, fromUserId, toUserId, tweet, "")
            }*/
          }
        //Schedule again
        import context.dispatcher
        //The below code gets a random time between 1 and 5 (both inclusive) milliseconds
        val time:Int = Random.nextInt(5 - 1 + 1) + 1;
        val fd = scala.concurrent.duration.FiniteDuration(time, "milliseconds")
        context.system.scheduler.scheduleOnce(fd, self, StartTweeting())
	  
      case StartGetting() =>
        for (i <- 1 to 70)
        {
          var fromUserId:Int = Random.nextInt(endIndex - startIndex + 1) + startIndex;
          var flag:Int = Random.nextInt(2)
          if(flag == 0)
          {
            //Get last 20 personal/own tweets
            serverRef ! requestTweet(fromUserId, 0, "", self)
          }
          else if(flag == 1)
          {
            //Get last 20 hashtagged tweets
          
            //Get a random hashtag from the existing hashtags
            var randomHashtagId:Int = Random.nextInt(100)
            val hashtag:String = hashtags.getOrElse(randomHashtagId, "#DefaultHashtag")
            serverRef ! requestTweet(fromUserId, 1, hashtag, self)
          }
          /*else if(flag == 2)
          {
            //Get last 20 timeline tweets
            serverRef ! requestTweet(fromUserId, flag, "", self)
          }
          else if(flag == 3)
          {
            //Get last 20 @mentions tweets
            serverRef ! requestTweet(fromUserId, flag, "", self)
          }*/
        }
        //Schedule again
        import context.dispatcher
        //The below code gets a random time between 1 and 5 (both inclusive) milliseconds
        val time:Int = Random.nextInt(5 - 1 + 1) + 1;
        val fd = scala.concurrent.duration.FiniteDuration(time, "milliseconds")
        context.system.scheduler.scheduleOnce(fd, self, StartGetting())

      case StartPeakGeneration() =>
          var flag:Int = Random.nextInt(2)
          if(flag == 0)
          {
            //Simulate peak by making some random users tweet for a particular hashtag
            var randomHashtagId:Int = Random.nextInt(100)
            val hashtag:String = hashtags.getOrElse(randomHashtagId, "#DefaultHashtag")
            for (i <- 1 to 150)
            {
              //Get a random user id
              var fromUserId:Int = Random.nextInt(endIndex - startIndex + 1) + startIndex;
              var tweet:String = randomCharactersString(5) + " " + hashtag + " " + randomCharactersString(5)
              serverRef ! receiveTweet(2, fromUserId, -1, tweet, hashtag)
            }
          }
          else if(flag == 1)
          {
            //Tweet an important tweet
            var fromUserId:Int = Random.nextInt(endIndex - startIndex + 1) + startIndex;
            var tweet:String = randomCharactersString(10)
            serverRef ! receiveTweet(3, fromUserId, -1, tweet, "")
            //Simulate peak by making some random users retweet an important tweet by adding via @fromUserId
            var retweet:String = randomCharactersString(10) + " (via @" + fromUserId.toString() + ")"
            for (i <- 1 to 150)
            {
              var frmUserId:Int = Random.nextInt(endIndex - startIndex + 1) + startIndex;
              //Make sure that the retweeting user (frmUserId) is not equal to the original user (fromUserId)
              if(frmUserId != fromUserId)
                serverRef ! receiveTweet(3, frmUserId, fromUserId, retweet, "")
            }
          }

        //Schedule again
        import context.dispatcher
        //The below code gets a random time between 1 and 5 (both inclusive) milliseconds
        //val time:Int = Random.nextInt(5 - 1 + 1) + 1;
        val fd = scala.concurrent.duration.FiniteDuration(5000, "milliseconds")
        context.system.scheduler.scheduleOnce(fd, self, StartPeakGeneration())

      case GetTweets(tweets) =>
        //Got tweets from server
        //Do nothing
    }
  }
}
