Play2.x module for Authentication and Authorization [![Build Status](https://secure.travis-ci.org/t2v/play2-auth.png)](http://travis-ci.org/t2v/play2-auth)
===========================================================

これは Play2.x のアプリケーションに認証/認可の機能を手軽に組み込むためのモジュールです。

対象
---------------------------------------

このモジュールは __Play2.x__ の __Scala__版を対象としています。
Java版には [Deadbolt 2](https://github.com/schaloner/deadbolt-2) というモジュールがありますので
こちらも参考にして下さい。

Play2.2.1 で動作確認をしています。

動機
---------------------------------------

### 安全性
 
標準で提供されている `Security` トレイトでは、ユーザを識別する識別子を規定していません。

サンプルアプリケーションのように、E-mailアドレスやユーザIDなどを識別子として利用した場合、
万が一Cookieが流出した場合に、即座にSessionを無効にすることができません。

このモジュールでは、暗号論的に安全な乱数生成器を使用してセッション毎にuniqueなSessionIDを生成します。
万が一Cookieが流失した場合でも、再ログインによるSessionIDの無効化やタイムアウト処理を行うことができます。

### 柔軟性

標準で提供されている `Security` トレイトでは、認証後に `Action` を返します。

これでは認証/認可以外にも様々なAction合成を行いたい場合にネストが深くなって非常に記述性が低くなります。

このモジュールでは `Either[PlainResult, User]` を返すインターフェイスを用意することで、
柔軟に他の操作を組み合わせて使用することができます。


以前のバージョン
---------------------------------------

Play2.1.x 向けの使用方法は [0.10.1 README](https://github.com/t2v/play2-auth/blob/release0.10.1/README.ja.md)をご参照ください。
Play2.0.x 向けの使用方法は [0.7 README](https://github.com/t2v/play2-auth/blob/release0.7/README.ja.md)をご参照ください。

Play2.1以前をお使いの方へ
---------------------------------------

<strong style="font-size: 200%; color: red;">Play2.2 から `Result` が非推奨になりました。その影響で play2.auth のインターフェイスも変更されています。</strong>

<strong style="font-size: 200%;">0.10.1以前からバージョンアップを行う方はご注意ください。</strong>

導入
---------------------------------------

`Build.scala` もしくは `build.sbt` にライブラリ依存性定義を追加します。

        "jp.t2v" %% "play2-auth"      % "0.11.0",
        "jp.t2v" %% "play2-auth-test" % "0.11.0" % "test"

For example: `Build.scala`

```scala
  val appDependencies = Seq(
    "jp.t2v" %% "play2-auth"      % "0.11.0-SNAPSHT",
    "jp.t2v" %% "play2-auth-test" % "0.11.0-SNAPSHT" % "test"
  )
```

このモジュールはシンプルな Scala ライブラリとして作成されています。 `play.plugins` ファイルは作成する必要ありません。


使い方
---------------------------------------

1. `app/controllers` 以下に `jp.t2v.lab.play2.auth.AuthConfig` を実装した `trait` を作成します。

    ```scala
    // (例)
    trait AuthConfigImpl extends AuthConfig {

      /**
       * ユーザを識別するIDの型です。String や Int や Long などが使われるでしょう。
       */
      type Id = String

      /**
       * あなたのアプリケーションで認証するユーザを表す型です。
       * User型やAccount型など、アプリケーションに応じて設定してください。
       */
      type User = Account

      /**
       * 認可(権限チェック)を行う際に、アクション毎に設定するオブジェクトの型です。
       * このサンプルでは例として以下のような trait を使用しています。
       *
       * sealed trait Permission
       * case object Administrator extends Permission
       * case object NormalUser extends Permission
       */
      type Authority = Permission

      /**
       * CacheからユーザIDを取り出すための ClassTag です。
       * 基本的にはこの例と同じ記述をして下さい。
       */
      val idTag: ClassTag[Id] = classTag[Id]

      /**
       * セッションタイムアウトの時間(秒)です。
       */
      val sessionTimeoutInSeconds: Int = 3600

      /**
       * ユーザIDからUserブジェクトを取得するアルゴリズムを指定します。
       * 任意の処理を記述してください。
       */
      def resolveUser(id: Id)(implicit ctx: ExecutionContext): Future[Option[User]] = Account.findByIdAsync(id)

      /**
       * ログインが成功した際に遷移する先を指定します。
       */
      def loginSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext): Future[SimpleResult] =
        Future.successful(Redirect(routes.Message.main))

      /**
       * ログアウトが成功した際に遷移する先を指定します。
       */
      def logoutSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext): Future[SimpleResult] =
        Future.successful(Redirect(routes.Application.login))

      /**
       * 認証が失敗した場合に遷移する先を指定します。
       */
      def authenticationFailed(request: RequestHeader)(implicit ctx: ExecutionContext): Future[SimpleResult] =
        Future.successful(Redirect(routes.Application.login))

      /**
       * 認可(権限チェック)が失敗した場合に遷移する先を指定します。
       */
      def authorizationFailed(request: RequestHeader)(implicit ctx: ExecutionContext): Future[SimpleResult] = 
        Future.successful(Forbidden("no permission"))

      /**
       * 権限チェックのアルゴリズムを指定します。
       * 任意の処理を記述してください。
       */
      def authorize(user: User, authority: Authority)(implicit ctx: ExecutionContext): Future[Boolean] = Future.successful {
        (user.permission, authority) match {
          case (Administrator, _) => true
          case (NormalUser, NormalUser) => true
          case _ => false
        }
      }

      /**
       * SessionID Cookieにsecureオプションを指定するか否かの設定です。
       * デフォルトでは利便性のために false になっていますが、
       * 実際のアプリケーションでは true にすることを強く推奨します。
       */
      override lazy val cookieSecureOption: Boolean = play.api.Play.current.configuration.getBoolean("auth.cookie.secure").getOrElse(true)

    }
    ```

1. 次にログイン、ログアウトを行う `Controller` を作成します。
   この `Controller` に、先ほど作成した `AuthConfigImpl` トレイトと、
   `jp.t2v.lab.play2.auth.LoginLogout` トレイトを mixin します。

    ```scala
    object Application extends Controller with LoginLogout with AuthConfigImpl {

      /** ログインFormはアプリケーションに応じて自由に作成してください。 */
      val loginForm = Form {
        mapping("email" -> email, "password" -> text)(Account.authenticate)(_.map(u => (u.email, "")))
          .verifying("Invalid email or password", result => result.isDefined)
      }

      /** ログインページはアプリケーションに応じて自由に作成してください。 */
      def login = Action { implicit request =>
        Ok(html.login(loginForm))
      }

      /**
       * ログアウト処理では任意の処理を行った後、
       * gotoLogoutSucceeded メソッドを呼び出した結果を返して下さい。
       *
       * gotoLogoutSucceeded メソッドは Future[SimpleResult] を返します。
       * 以下のようにflashingなどを追加することもできます。
       *
       *   gotoLogoutSucceeded.map(_.flashing(
       *     "success" -> "You've been logged out"
       *   ))
       */
      def logout = Action.async { implicit request =>
        // do something...
        gotoLogoutSucceeded
      }

      /**
       * ログイン処理では認証が成功した場合、
       * gotoLoginSucceeded メソッドを呼び出した結果を返して下さい。
       *
       * gotoLoginSucceeded メソッドも gotoLogoutSucceeded と同じく Future[SimpleResult] を返します。
       * 任意の処理を追加することも可能です。
       */
      def authenticate = Action.async { implicit request =>
        loginForm.bindFromRequest.fold(
          formWithErrors => Future.successful(BadRequest(html.login(formWithErrors))),
          user => gotoLoginSucceeded(user.get.id)
        )
      }

    }
    ```

1. 最後は、好きな `Controller` に 先ほど作成した `AuthConfigImpl` トレイトと
   `jp.t2v.lab.play2.auth.AuthElement` トレイト を mixin すれば、認証/認可の仕組みを導入することができます。

    ```scala
    object Message extends Controller with AuthElement with AuthConfigImpl {

      // StackAction の 引数に権限チェック用の (AuthorityKey, Authority) 型のオブジェクトを指定します。
      // 第二引数に RequestWithAttribute[AnyContent] => Result な関数を渡します。

      // AuthElement は loggedIn[A](implicit RequestWithAttribute[A]): User というメソッドをもっています。
      // このメソッドから認証/認可済みのユーザを取得することができます。

      def main = StackAction(AuthorityKey -> NormalUser) { implicit request =>
        val user = loggedIn
        val title = "message main"
        Ok(html.message.main(title))
      }

      def list = StackAction(AuthorityKey -> NormalUser) { implicit request =>
        val user = loggedIn
        val title = "all messages"
        Ok(html.message.list(title))
      }

      def detail(id: Int) = StackAction(AuthorityKey -> NormalUser) { implicit request =>
        val user = loggedIn
        val title = "messages detail "
        Ok(html.message.detail(title + id))
      }

      // このActionだけ、Administrator でなければ実行できなくなります。
      def write = aStackAction(AuthorityKey -> Administrator) { implicit request =>
        val user = loggedIn
        val title = "write message"
        Ok(html.message.write(title))
      }

    }
    ```

テスト
---------------------------------------

play2.auth では、version 0.8 からテスト用のサポートを提供しています。

`FakeRequest` を使って `Controller` のテストを行う際に、
ログイン状態のユーザを指定することができます。

```scala
package test

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import controllers.{AuthConfigImpl, Messages}
import jp.t2v.lab.play2.auth.test.Helpers._

class ApplicationSpec extends Specification {

  object config extends AuthConfigImpl

  "Messages" should {
    "return list when user is authorized" in new WithApplication {
      val res = Messages.list(FakeRequest().withLoggedIn(config)(1))
      contentType(res) must equalTo("text/html")
    }
  }

}
```

1. まず `jp.t2v.lab.play2.auth.test.Helpers._` を import します。
1. 次にテスト対象に mixin されているものと同じ `AuthConfigImpl` のインスタンスを生成します。

        object config extends AuthConfigImpl

1. `FakeRequest` の `withLoggedIn` メソッドを呼び出します。
    * 第一引数には、先ほど定義した `AuthConfigImpl` インスタンス
    * 第二引数には、このリクエストがログインしている事にする、対象のユーザIDを指定します。


以上で play2.auth を使用したコントローラのテストを行うことができます。



高度な使い方
---------------------------------------

### リクエストパラメータに応じて権限判定を変更する

例えば SNS のようなアプリケーションでは、メッセージの編集といった機能があります。

しかしこのメッセージ編集は、自分の書いたメッセージは編集可能だけども、
他のユーザが書いたメッセージは編集禁止にしなくてはいけません。

そういった場合にも以下のように `Authority` を関数にすることで簡単に対応が可能です。

```scala
trait AuthConfigImpl extends AuthConfig {

  // 他の設定省略

  type Authority = User => Future[Boolean]

  def authorize(user: User, authority: Authority)(implicit ctx: ExecutionContext): Future[Boolean] = authority(user)

}
```

```scala
object Application extends Controller with AuthElement with AuthConfigImpl {

  private def sameAuthor(messageId: Int)(account: Account): Future[Boolean] =
    Message.getAutherAsync(messageId).map(_ == account)

  def edit(messageId: Int) = StackAction(AuthorityKey -> sameAuthor(messageId)) { request =>
    val target = Message.findById(messageId)
    Ok(html.message.edit(messageForm.fill(target)))
  }

}
```


### ログイン後、認証直前にアクセスしていたページに遷移する

アプリケーションの任意のページにアクセスしてきた際に、
未ログイン状態であればログインページに遷移し、
ログインが成功した後に最初にアクセスしてきたページに戻したい、といった要求があります。

その場合も以下のようにするだけで簡単に実現できます。

```scala
trait AuthConfigImpl extends AuthConfig {

  // 他の設定省略

  def authenticationFailed(request: RequestHeader)(implicit ctx: ExecutionContext): Future[SimpleResult] =
    Future.successful(Redirect(routes.Application.login).withSession("access_uri" -> request.uri))

  def loginSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext): Future[SimpleResult] = {
    val uri = request.session.get("access_uri").getOrElse(routes.Message.main.url)
    Future.successful(Redirect(uri).withSession(request.session - "access_uri"))
  }

}
```


### ログイン状態と未ログイン状態で表示を変える

トップページなどにおいて、未ログイン状態でも画面を正常に表示し、
ログイン状態であればユーザ名などを表示する、といったことがしたい場合、
以下のように `AuthElement` の代わりに `OptionalAuthElement` を使用することで実現することができます。

`OptionalAuthElement` を使用する場合、`Authority` は必要ありません。

```scala
object Application extends Controller with OptionalAuthElement with AuthConfigImpl {

  // maybeUser is an Option[User] instance.
  def index = StackAction { implicit request =>
    val maybeUser: Option[User] = loggedIn
    val user: User = maybeUser.getOrElse(GuestUser)
    Ok(html.index(user))
  }

}
```


### 認証だけ行って認可は行わない。

認証だけ行うこともできます。

`AuthElement` の代わりに `AuthenticationElement` を使うだけです。
この場合、 `AuthorityKey` の指定は必要ありません。

```scala
object Application extends Controller with AuthenticationElement with AuthConfigImpl {

  def index = StackAction { implicit request =>
    val user: User = loggedIn
    Ok(html.index(user))
  }

}
```


### Ajaxリクエスト時の認証失敗で401を返す

通常のアクセスで認証が失敗した場合にはログイン画面にリダイレクトさせたいけれども、
Ajaxリクエストの場合には単に401を返したい場合があります。

その場合でも以下の様に `authenticationFailed` で分岐すれば実現することができます。


```scala
def authenticationFailed(request: RequestHeader)(implicit ctx: ExecutionContext) = Future.successful {
  request.headers.get("X-Requested-With") match {
    case Some("XMLHttpRequest") => Unauthorized("Authentication failed")
    case _ => Redirect(routes.Application.login)
  }
}
```


### 他のAction操作と合成する

[stackable-controller](https://github.com/t2v/stackable-controller) の仕組みを使用します。

例えば、CSRF対策で各Actionでトークンのチェックをしたい、としましょう。

全てのActionで毎回チェックロジックを書くのは大変なので、以下のようなトレイトを作成します。

```scala
trait TokenValidateElement extends StackableController {
    self: Controller =>

  // Token の発行処理は省略

  private val tokenForm = Form("token" -> text)

  private def validateToken(request: Request[AnyContent]): Boolean = (for {
    tokenInForm <- tokenForm.bindFromRequest(request).value
    tokenInSession <- request.session.get("token")
  } yield tokenInForm == tokenInSession).getOrElse(false)

  override proceed[A](reqest: RequestWithAttributes[A])(f: RequestWithAttributes[A] => Future[SimpleResult]): Future[SimpleResult] = {
    if (validateToken(request)) super.proceed(request)(f)
    else Future.successful(BadRequest)
  }

}
```

この `TokenValidateElement` トレイトと `AuthElement` トレイトを両方mixinすることで、
CSRFトークンチェックと認証/認可を両方行うことができます。

```scala
object Application extends Controller with TokenValidateElement with AuthElement with AuthConfigImpl {

  // Token の発行処理は省略

  def page1 = StackAction(AuthorityKey -> NormalUser) { implicit request =>
    // do something
    Ok(html.page1("result"))
  }

  def page2 = StackAction(AuthorityKey -> NormalUser) { implicit request =>
    // do something
    Ok(html.page2("result"))
  }

}
```

### 非同期サポート

効率的なアプリケーションを作成するため、昨今ではReactiveなアプローチが人気を博しています。
Playはこういった非同期なアプローチが得意であり、[ReactiveMongo](http://reactivemongo.org/) や [ScalikeJDBC-Async](https://github.com/seratch/scalikejdbc-async) などといった非同期なライブラリを上手に使用する事ができます。

`StackAction` の代わりに `AsyncStack` を使用することで、 Future[SimpleResult] を返すアクションを簡単につくることができます。


```scala
trait HogeController extends AuthElement with AuthConfigImpl {

  def hoge = AsyncStack { implicit req =>
    val messages: Future[Seq[Message]] = AsyncDB.withPool { implicit s => Message.findAll }
    messages.map(Ok(html.view.messages(_)))
  }

}
```

### Stateless

このモジュールの標準実装はステートフルな実装になっています。
Play framefork が推奨するステートレスなポリシーを尊重したくはあるのですが、
ステートレスにすると次のようなセキュリティリスクが存在するため、標準では安全側に倒してあります。

例えば、インターネットカフェなどでサービスにログインし、
ログアウトするのを忘れて帰宅してしまった、といった場合。
ステートレスではその事実に気付いても即座にそのSessionを無効にすることができません。
標準実装ではログイン時に、それより以前のSessionを無効にしてます。
したがってこの様な事態に気付いた場合、即座に再ログインすることでSessionを無効化することができます。

このようなリスクを踏まえ、それでもステートレスにしたい場合、
以下のように設定することでステートレスにすることができます。

```scala
trait AuthConfigImpl extends AuthConfig {

  // 他の設定省略

  override lazy val idContainer: IdContainer[Id] = new CookieIdContainer[Id]

}
```

`IdContainer` は SessionID および UserID を紐付ける責務を負っています。
この実装を切り替えることで、例えば RDBMS に認証情報を登録するといった事も可能です。

なお、`CookieIdContainer` ではSessionタイムアウトは未サポートとなっています。


サンプルアプリケーション
---------------------------------------

1. `git clone https://github.com/t2v/play2-auth.git`
1. `cd play2-auth`
1. `play "project sample" play run`
1. ブラウザで `http://localhost:9000/` にアクセス
    1. 「Database 'default' needs evolution!」と聞かれるので `Apply this script now!` を押して実行します
    1. 適当にログインします
    
        アカウントは以下の3アカウントが登録されています。
        
            Email             | Password | Permission
            alice@example.com | secret   | Administrator
            bob@example.com   | secret   | NormalUser
            chris@example.com | secret   | NormalUser


注意事項
---------------------------------------

このモジュールは Play2.x の [Cache API](http://www.playframework.org/documentation/2.0/ScalaCache) を利用しています。

標準実装の [Ehcache](http://ehcache.org) では、サーバを分散させた場合に正しく認証情報を扱えない場合があります。

サーバを分散させる場合には [Memcached Plugin](https://github.com/mumoshu/play2-memcached) 等を利用してください。


ライセンス
---------------------------------------

このモジュールは Apache Software License, version 2 の元に公開します。

詳しくは `LICENSE` ファイルを参照ください。

