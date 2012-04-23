package models

sealed abstract class Permission(private val order: Int) extends Ordered[Permission] {
  def compare(that: Permission) = this.order compare that.order
}

case object Administrator extends Permission(0)
case object NormalUser extends Permission(1)
