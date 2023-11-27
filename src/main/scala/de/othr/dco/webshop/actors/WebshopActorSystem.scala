package de.othr.dco.webshop.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import de.othr.dco.webshop.actors.BasketActor.{AllItemsForUserWithPayment, AllItemsForUserWithoutPayment}
import de.othr.dco.webshop.models.Entities.{Item, Order, Payment, User}

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object WebshopActorSystem {
  sealed trait UserCommandMessage
  case class AddItemToBasket(user: User, item: Item) extends UserCommandMessage
  case class MakeOrder(user: User) extends UserCommandMessage
  case class AdaptedBasketResponse(response: BasketActor.BasketResponse) extends UserCommandMessage

  case class AdaptedPaymentResponse(responseFromPayment: String, order: Order) extends UserCommandMessage

  def apply(): Behavior[UserCommandMessage] = Behaviors.setup(
    setupCxt => {
      //Spawns needed actors
      val mapperRef: ActorRef[BasketActor.BasketResponse] = setupCxt.messageAdapter(response => AdaptedBasketResponse(response))

      val basketActor: ActorRef[BasketActor.BasketRequest] = setupCxt.spawn(BasketActor(), "basket-actor")
      val paymentActor: ActorRef[PaymentActor.PaymentRequest] = setupCxt.spawn(PaymentActor(), "payment-actor")
      handleUserMessage(setupCxt, basketActor, mapperRef, paymentActor)
    }
  )


  private def handleUserMessage(context: akka.actor.typed.scaladsl.ActorContext[UserCommandMessage],
                                basketActor: ActorRef[BasketActor.BasketRequest],
                                mapperRef: ActorRef[BasketActor.BasketResponse],
                                paymentActor: ActorRef[PaymentActor.PaymentRequest]): Behavior[UserCommandMessage] =
    Behaviors.receiveMessage(
      msg => msg match {

        //This is request handling for user input
        case AddItemToBasket(user, item) => {
          println(s"[WebshopActorSystem] User: ${user.userName} --- Item: ${item.itemName}")
          basketActor ! BasketActor.AddItemToUserBasket(user, item, mapperRef)
          Behaviors.same
        }
        case MakeOrder(user) =>{
          println(s"[WebshopActorSystem] ${user.userName} wants to make an order.")
          basketActor ! BasketActor.GetAllItemForUser(user, mapperRef)
          Behaviors.same
        }
        //--------------------------------------

        //This is response handling for adapter responses
        case AdaptedBasketResponse(basketResponse) => {
          //This is only needed due to param basketResponse being of type BasketActor.BasketResponse
          //So another case match is needed for all implementations of BasketActor.BasketResponse
          //In this simple example only one case is implemented
          basketResponse match {
            case AllItemsForUserWithoutPayment(itemList) =>{
              println("[WebshopActorSystem] print received Basket ")
              itemList.foreach(x => println(x.itemName))
              println("[WebshopActorSystem] --------------------")
              Behaviors.same
            }
            case AllItemsForUserWithPayment(itemList) => {
              println("[WebshopActorSystem] received Basket for Payment.")

              //TODO: Implement how much the itemList would cost the user
              val payment = new Payment(3.4f)

              //This is the 'Adaptor'-implementation
              implicit val timeout: Timeout = 5.seconds

              context.ask(paymentActor, ref => PaymentActor.CollectPayment.apply(payment, ref)){
                case Success(PaymentActor.PaymentCollected(message)) => AdaptedPaymentResponse(message, new Order(itemList, payment))
                case Failure(_) => AdaptedPaymentResponse("Timeout failure", new Order(List[Item](), new Payment(0.0f)))
              }
              //-------------------------------------
              Behaviors.same
            }
          }
        }
        //-------------------
        case AdaptedPaymentResponse(responseFromPaymentActor, order) => {
          println()
          println()
          println()
          println("[WebshopActorSystem] Received invoice")
          println(s"[WebshopActorSystem] Invoice status: ${responseFromPaymentActor}")
          println("-------------- INVOICE ----------------")
          order.itemList.foreach(x => println(s"Item: ${x.itemName}"))
          println()
          println(s"Total price: ${order.payment.price}")
          println("---------------------------------------")
          Behaviors.same
        }
      }
    )

}
