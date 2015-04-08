import akka.actor._
import scala.util.Random
import scala.collection.mutable.HashMap
import scala.collection.immutable.TreeMap
import scala.util.control.Breaks._

case class StartSendingRequests(nodeMap: TreeMap[String,ActorRef], distanceNodes: HashMap[Int,String], numNodes: Int, numRequests: Int,failureNodes:HashMap[String,Int])
case class InitializePastryActor(nodeId: String,numRequests: Int,nodeMap: TreeMap[String,ActorRef],distanceNodes: HashMap[Int,String],routingTable: Array[Array[String]],leafSet: Array[String],neighborhoodSet :Array[String])
case class RouteRequest(id: String,masterActor: ActorRef,msg: String,count: Int, failureNodes:HashMap[String,Int])
case class StopRouting(numHops: Int)
case class Shutdown(avgHops: Float)

class PastryActor extends Actor{

  var nodeId: String=null
  var numRequests: Int=0
  var nodeMap=TreeMap.empty[String,ActorRef]
  var distanceNodes=HashMap.empty[Int,String]
  var routingTable=Array.ofDim[String](8,4)
  var leafSet=Array.ofDim[String](8)
  var neighborhoodSet=Array.ofDim[String](8)
	
  def receive = {
	
  case InitializePastryActor(nodeId,numRequests,nodeMap,distanceNodes,routingTable,leafSet,neighborhoodSet) =>
		
    this.nodeId=nodeId
    this.numRequests=numRequests
    this.nodeMap=nodeMap
    this.distanceNodes=distanceNodes
    this.routingTable=routingTable
    this.leafSet=leafSet
    this.neighborhoodSet=neighborhoodSet
	
  case RouteRequest(id,masterActor,msg,numHops,failureNodes)=>

    var nextNodeId: String=null
    var i: Int=0
    var distance: Int=0
    var flag: Int=0
    var destIdFlag: Int=0

    try 
    { 
      //Stop routing if the message has reached the destination
      if(nodeId.equals(id))
      {
        masterActor ! StopRouting(numHops)
      }
      else
      {
        //Get the number of actual elements in the leaf set.
        //If total number of nodes in the network is >= 9, then there would be 8 elements in the leaf set.
        //If total number of nodes in the network is < 9, then there would be less than 8 elements in the leaf set.
        var leafSetSize : Int = 8
        if(nodeMap.size < 9)
          leafSetSize = nodeMap.size-1
        
        //If the key (id) lies within the leafSet range, then route the 
        //message to the node whose id is numerically closest to the key (id).
        distance = -1
        if((leafSet(0) != null) && (Integer.parseInt(leafSet(0)) <= Integer.parseInt(id) && Integer.parseInt(leafSet(leafSetSize-1)) >= Integer.parseInt(id)))
        {
          if(!failureNodes.contains(leafSet(0)))
          {
            nextNodeId=leafSet(0)
            distance=math.abs(Integer.parseInt(leafSet(0))-Integer.parseInt(id))
            flag=1
          }
          else
          {
            if(leafSet(0).equals(id))
            {
              masterActor ! StopRouting(numHops)
              destIdFlag = 1
            }
          }
          if(destIdFlag == 0)
          {
	          for(i<-1 until 8 by 1)
	          {
	            if((distance == -1) || ((leafSet(i)!=null) && (math.abs(Integer.parseInt(leafSet(i))-Integer.parseInt(id)) < distance)))
	            {
	              if(!failureNodes.contains(leafSet(i)))
	              {
	                distance=math.abs(Integer.parseInt(leafSet(i))-Integer.parseInt(id))
	                nextNodeId=leafSet(i)
	                flag=1
	              }
	              else
		          {
		            if(leafSet(i).equals(id))
		            {
		              masterActor ! StopRouting(numHops)
		              destIdFlag = 1
		              flag = 0
		            }
		          }
	            }
	          }
          }
        }
        if(flag == 0 && destIdFlag == 0)
        {  
          var row: Int=0
          var column: Int=0
    
          //Get the length (i.e. row number) of common prefix shared between D (id) and A (nodeId). 
          //Start from row 1, because row 0 will not share any common prefix.
          row=0
          for(i<-1 until 8 by 1)
          {
            if(id.substring(0,i)==nodeId.substring(0,i))
            {
              row=i
            }
          }
          //Get the row'th digit from the key (id)
          column=Integer.parseInt(id.charAt(row).toString())
          //If there exists a node id in that particular row and column, then route message to that node
          if(routingTable(row)(column)!=null && !routingTable(row)(column).equals("00000000") && row!=0)
          {
            if(!failureNodes.contains(routingTable(row)(column)))
            {
              nextNodeId=routingTable(row)(column)
              flag=1
            }
            else
	          {
	            if(routingTable(row)(column).equals(id))
	            {
	              masterActor ! StopRouting(numHops)
	              destIdFlag = 1
	            }
	          }
          }        	  
          if(flag == 0 && destIdFlag == 0)
          {
            //rare case
          
            //Get shl(D, A) (i.e. shl(id, nodeId))
            var l: Int=0 
            l=row
          
            //Get |A - D| (i.e. |nodeId - id|)
            distance = math.abs(Integer.parseInt(nodeId)-Integer.parseInt(id))
          
            //Check if there are numerically closer nodes in the leaf set
            for(i<-0 until 8 by 1)
            {
              //If the leaf node (L) and key (D) have a common prefix at least as long as l
              if((leafSet(i) != null) && (leafSet(i).substring(0,row).equals(id.substring(0,row))))
              { 
                //If |L - D| < previously calculated closest distance
                if(math.abs(Integer.parseInt(leafSet(i))-Integer.parseInt(id)) < distance)
                {
                  if(!failureNodes.contains(leafSet(i)))
                  {
                    distance=math.abs(Integer.parseInt(leafSet(i))-Integer.parseInt(id))
                    nextNodeId=leafSet(i)
                    flag=1
                  }
                  else
                  {
                    if(leafSet(i).equals(id))
                    {
                      masterActor ! StopRouting(numHops)
                      destIdFlag = 1
                    }
                  }
                }
              }
            }
            if(destIdFlag == 0)
            {
	            //Check if there are numerically closer nodes in the routing table
	            for(i<-0 until 8 by 1)
	            {
	              for(j<-0 until 4 by 1)
	              {
	                //If the routing table node (R) and key (D) have a common prefix at least as long as l
	                if((routingTable(i)(j)!=null) && (!routingTable(i)(j).equals("00000000")) && (routingTable(i)(j).substring(0,row).equals(id.substring(0,row))))
	                { 
	                  //If |R - D| < previously calculated closest distance
	                  if(math.abs(Integer.parseInt(routingTable(i)(j))-Integer.parseInt(id)) < distance)
	                  {
	                    if(!failureNodes.contains(routingTable(i)(j)))
	                    {
	                      distance=math.abs(Integer.parseInt(routingTable(i)(j))-Integer.parseInt(id))
	                      nextNodeId=routingTable(i)(j)
	                      flag=1
	                    }
	                    else
	                    {
		                    if(routingTable(i)(j).equals(id))
		                    {
		                      masterActor ! StopRouting(numHops)
		                      destIdFlag = 1
		                    }
	                    }
	                  }
	                }
	              }
	            }
	             if(destIdFlag == 0)
	             {
		            //Check if there are numerically closer nodes in the neighborhood set
		            for(i<-0 until 8 by 1)
		            {
		              //If the neighborhood node (M) and key (D) have a common prefix at least as long as l
		              if((neighborhoodSet(i)!=null) && (neighborhoodSet(i).substring(0,row).equals(id.substring(0,row))))
		              { 
		                //If |M - D| < previously calculated closest distance
		                if(math.abs(Integer.parseInt(neighborhoodSet(i))-Integer.parseInt(id)) < distance)
		                {
		                  if(!failureNodes.contains(neighborhoodSet(i)))
		                  {
		                    distance=math.abs(Integer.parseInt(neighborhoodSet(i))-Integer.parseInt(id))
		                    nextNodeId=neighborhoodSet(i)
		                    flag=1
		                  }
		                  else
		                  {
		                   if(neighborhoodSet(i).equals(id))
		                    {
		                      masterActor ! StopRouting(numHops)
		                      destIdFlag = 1
		                    }
		                  }
		                }
		              }
		            }
		
		            //No suitable routing node has been found. Therefore, stop routing.
		            if(flag==0 && destIdFlag == 0)
		            {
		              masterActor ! StopRouting(numHops)
		            }
	             }
            }
          }       	  
        }
        //Route message to next node
        if((flag == 1 && destIdFlag == 0) || (flag == 1 && destIdFlag == 1))
          nodeMap.apply(nextNodeId) ! RouteRequest(id,masterActor,msg,numHops+1,failureNodes)
      }
    }
    catch 
    {  
      case e : Exception => 
        if(e.equals("java.lang.NullPointerException")) 
        {
          masterActor ! StopRouting(numHops)
        }
    }
  }
}

class MasterActor extends Actor{

  var averageHops = Array.ofDim[Float](1)
  var nNodes: Int=0
  var nRequest: Int=0
  var nFailures: Int=0
  var nHops: Int=0
  var sum: Float=0
	
  def receive = {

    case StartSendingRequests(nodeMap,distanceNodes,numNodes,numRequests, failureNodes)=>
      println("Started sending requests.")
      nNodes = numNodes
      nRequest=numRequests
      nFailures=failureNodes.size
      var reqCount: Int=nRequest
      var k: Int=0
      for(i<-0 until numNodes by 1)
      {
        var sourceNode: String=distanceNodes.getOrElse(i,"none")
        if(!failureNodes.contains(sourceNode))
        {
          var destinationNode: String=null
  
          k=i+1
          if(k==numNodes)
            k = 0
        
          while(reqCount > 0)
          {     
		    destinationNode=distanceNodes.getOrElse(k,"none")
		    nodeMap.apply(sourceNode)! RouteRequest(destinationNode,self,"PastryMessage",1,failureNodes)
		    reqCount=reqCount-1
		  
		    k=k+1 
		    if(k==numNodes) 
		      k = 0
          }
        }
        //Re-initialize reqCount for next node
        reqCount=nRequest
      }

    case StopRouting(numHops)=>
      nHops+=numHops
      sum+=1
      //println(sum)
      if(sum==(nNodes-nFailures)*nRequest)
	  {
        println("Total number of requests completed: " + sum)
        self ! Shutdown(nHops/sum)
      }
					
    case Shutdown(avgHops) =>
      averageHops(0) = avgHops
      printf("Average number of hops: %.2f\n",averageHops(0))
      context.system.shutdown
    }
}

object project3bonus
{
  var numNodes: Int=0
  var numRequests: Int=0
  var numFailures: Int = 0
	
  //Main Method
  def main(args: Array[String])
  {
    numNodes=args(0).toInt
    numRequests=args(1).toInt
    numFailures=args(2).toInt
    BuildNetwork(numNodes, numRequests, numFailures)
  }
  
  //Build Network
  def BuildNetwork(numNodes: Int, numRequests: Int, numFailures: Int)
  {
    var j: Int=0
    var k: Int=0
    var n: Int=numNodes
    var index: Int=0
    var isNodeInserted: Int=0
    var random=new Random
    var nodeId: String=""		
    var ch: Char=' '
    var nodes=List.empty[String]
    var nodeMap=TreeMap.empty[String,ActorRef]
    var distanceNodes=HashMap.empty[Int,String]
    val system = ActorSystem("PastrySystem")
		
    //Generate nodeMap and distanceNodes
    k=8
    for(i<-0 until n by 1)
    {
      val pActor = system.actorOf(Props(new PastryActor()))
      while(isNodeInserted==0)
      {
        while(k!=0)
        {
          k-=1
          nodeId+=random.nextInt(4).toString()                     
        }
        if(!nodeMap.contains(nodeId))
        {
          nodeMap=nodeMap.insert(nodeId,pActor)
          distanceNodes.put(i,nodeId)
          isNodeInserted=1
        }
        else
        {
          //If the generated nodeId is already present in the map, then generate another one
          k=8
          nodeId=""
        }
      }
			
      k=8   
      nodeId=""
      isNodeInserted=0
    }     
	
    //Get a sorted list of all keys (nodeIds)
    nodes=nodeMap.keySet.toList
    var id: String=""
    var neighborIndicator:Int = 0
    var leafIndicator:Int = 0
		
    //Generate Leaf Sets, Neighborhood Sets and Routing Tables for each of the nodes
    for(i<-0 until n by 1)
    {
      var leafSet=Array.ofDim[String](8)
      var routingTable=Array.ofDim[String](8,4)
      var neighborhoodSet=Array.ofDim[String](8)
      
      id=distanceNodes.getOrElse(i,"none")
      index=nodes.indexOf(id)
      
      //Generate Neighborhood Set
      neighborIndicator = i+1  
      if(neighborIndicator==n)
        neighborIndicator = 0
			
      for(j<-0 until 8 by 1)
      {      		  
        neighborhoodSet(j)=distanceNodes.getOrElse(neighborIndicator,"none")
        neighborIndicator = neighborIndicator+1
				
        if(neighborIndicator==n)
		  neighborIndicator = 0
      }
			
      //Generate Leaf Set 
      leafIndicator = i
      if(n < 9)
        leafIndicator=0
      else if((index-4)<0) 
        leafIndicator=0 
      else if((index+4)>n-1) 
        leafIndicator=n-9 
      else 
        leafIndicator=index-4 
      breakable
      {
        for(j<-0 until 8 by 1) 
        { 
          if(leafIndicator==index) 
            leafIndicator+=1 
          if(leafIndicator == n)
            break;
          leafSet(j)=nodes(leafIndicator) 
          leafIndicator=leafIndicator+1 
        }
      }
      
      //Generate Routing Table			
      for(k<-0 until 8 by 1)
      {
        j=0
        ch=id.charAt(k)
        routingTable(k)(Integer.parseInt(ch.toString()))="00000000"
				  
        breakable
        {   
          for((key,value)<-nodeMap)
          {		
            if(key!=id)
            {
              //If kth character is different in key and id && If first k characters (i.e. 0 to k-1 characters) are same in key and id
              if(key.charAt(k)!= ch && key.substring(0,k).equals(id.substring(0,k)))
              {
                //If jth column is equal to kth character in the id, then skip that column
                if(j==Integer.parseInt(ch.toString()))
                {
                  j=j+1
                  if(j==4)
                    break
                }
                else
                {
                  //If jth column is equal to kth character in the key, then store it
                  if(j==Integer.parseInt(key.charAt(k).toString()))
                  {           
                    routingTable(k)(j)=key
                    j=j+1
                  }
                }			      		        
              }
              if(j==4)
                break
            }			
          }			
        }
      }
      //Initialize actor
      nodeMap.apply(id)! InitializePastryActor(id,numRequests,nodeMap,distanceNodes,routingTable,leafSet,neighborhoodSet)
    }
    println("Completed building network.")
    
    var failureNodes=HashMap.empty[String,Int]
    var randomFailureNode:String = null
    
    for(i<-0 until numFailures by 1)
      {
        randomFailureNode = distanceNodes.getOrElse(i, "none")
        failureNodes.put(randomFailureNode,i)
      }
    
    
    //Start sending requests
    val mActor = system.actorOf(Props(new MasterActor()))
    mActor ! StartSendingRequests(nodeMap,distanceNodes,numNodes,numRequests,failureNodes)
  }
}
