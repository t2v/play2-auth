package jp.t2v.lab.play2.auth.sample

sealed trait Permission

object Permission {

  case object Administrator extends Permission
  case object NormalUser extends Permission

  def valueOf(value: String): Permission = value match {
    case "Administrator" => Administrator
    case "NormalUser"    => NormalUser
    case _ => throw new IllegalArgumentException()
  }

}
