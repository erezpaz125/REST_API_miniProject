package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._
import scala.collection.mutable
import models.TodoListItem
import models.NewTodoListItem
import models.Player
import models.PlayerTable
import slick.jdbc.H2Profile.api._
import slick.jdbc.H2Profile
import scala.concurrent._

@Singleton
class TodoListController @Inject()(val controllerComponents: ControllerComponents)
extends BaseController {
    implicit val todoListJson = Json.format[TodoListItem]
    implicit val newTodoListJson = Json.format[NewTodoListItem]
    implicit val playerJson = Json.format[Player]

    // database stuff - //
    val db = Database.forConfig("h2mem")
    val playerTable = TableQuery[PlayerTable]
    val germanPlayersQuery = playerTable.filter(_.country === "Germany")
    val germanPlayers: Future[Seq[Player]] = db.run[Seq[Player]](germanPlayersQuery.result)
    // this doesn't increment id -
    

    // this increments id - 
    // val forceInsertAction = playerTable.forceInsert(player)


    // List stuff from the example //
      private var todoList = new mutable.ListBuffer[TodoListItem]()
    todoList += TodoListItem(1, "test", true)
    todoList += TodoListItem(2, "some other value", false)
    
    // def insertPlayer(player:Player) = Action 
    // {

    // }

    // curl localhost:9000/todo
    def getAll(): Action[AnyContent] = Action 
    {
        if (todoList.isEmpty) 
        {
            NoContent
        }   
        else 
        {
            Ok(Json.toJson(todoList))
        }
    }   

    def getById(itemId: Long) = Action 
    {
      val foundItem = todoList.find(_.id == itemId)
      foundItem match 
      {
        case Some(item) => Ok(Json.toJson(item))
        case None => NotFound
      }
    }

    def markAsDone(itemId: Long) = Action {
    val foundItem = todoList.find(_.id == itemId)
    foundItem match {
      case Some(item) =>
        val newItem = item.copy(isItDone = true)
        todoList.dropWhileInPlace(_.id == itemId)
        todoList += newItem
        Accepted(Json.toJson(newItem))
      case None => NotFound
    }
  }

  def deleteAllDone() = Action {
    todoList.filterInPlace(_.isItDone == false)
    Accepted
  }

  // curl -v -d "{\"description\": \"some new item\"}" -H "Content-Type:application/json" -X POST localhost:9000/todo
  def addNewItem() = Action 
    { implicit request =>
        val content = request.body
        val jsonObject = content.asJson

        val todoListItem: Option[NewTodoListItem] = jsonObject.flatMap(Json.fromJson[NewTodoListItem](_).asOpt)

        todoListItem match 
        {
            case Some(newItem) =>
                val nextId = todoList.map(_.id).max + 1
                val toBeAdded = TodoListItem(nextId, newItem.description, false)
                todoList += toBeAdded
                Created(Json.toJson(toBeAdded))
            case None =>
                BadRequest
        }

    }
    def addNewPlayer() = Action 
    { implicit request =>
        val content = request.body
        val jsonObject = content.asJson

        val newPlayer: Option[Player] = jsonObject.flatMap(Json.fromJson[Player](_).asOpt)

        newPlayer match 
        {
            case Some(newPlayer) =>
                // val nextId = todoList.map(_.id).max + 1
                val toBeAdded = Player(newPlayer.id, newPlayer.name,newPlayer.country)
                val insertPlayerQuery = playerTable += toBeAdded
                val insertResult:Future[Int] = db.run(insertPlayerQuery)
                Created(Json.toJson(toBeAdded))
            case None =>
                BadRequest
        }

    }

}

