package models

case object OppositeTurnException extends Exception
case object InvalidPointException extends Exception
case object InvalidPutException extends Exception
case object StoneAlreadyExistsException extends Exception
case object InvalidInputException extends Exception
case class UnknownAction(message: String) extends Exception(message)
