package models

sealed trait Permission
case object Administrator extends Permission
case object NormalUser extends Permission
