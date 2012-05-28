Play2.0 module for Authentication and Authorization
===========================================================

これは Play2.0 のアプリケーションに認証/認可の機能を手軽に組み込むためのモジュールです。

対象
---------------------------------------

このモジュールは __Play2.0__ の __Scala__版を対象としています。
Java版には [Deadbolt 2](https://github.com/schaloner/deadbolt-2) というモジュールがありますので
こちらも参考にして下さい。

Play2.0final および Play2.0.1 で動作確認をしています。

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


導入
---------------------------------------

1. `Build.scala` もしくは `build.sbt` にリポジトリ定義を追加します。

        resolvers += "t2v.jp repo" at "http://www.t2v.jp/maven-repo/"

1. `Build.scala` もしくは `build.sbt` にライブラリ依存性定義を追加します。
    1. 安定版

            "jp.t2v" %% "play20.auth" % "0.2"

    1. 開発版

            "jp.t2v" %% "play20.auth" % "0.3-SNAPSHOT"

For example: `Build.scala`

```scala
  val appDependencies = Seq(
    "jp.t2v" %% "play20.auth" % "0.1"
  )

  val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
    resolvers += "t2v.jp repo" at "http://www.t2v.jp/maven-repo/"
  )
```


使い方
---------------------------------------

1. `app/controllers` 以下に `jp.t2v.lab.play20.auth.AuthConfig` を実装した `trait` を作成します。

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
       * CacheからユーザIDを取り出すための ClassManifest です。
       * 基本的にはこの例と同じ記述をして下さい。
       */
      val idManifest: ClassManifest[Id] = classManifest[Id]
    
      /**
       * セッションタイムアウトの時間(秒)です。
       */
      val sessionTimeoutInSeconds: Int = 3600
    
      /**
       * ユーザIDからUserブジェクトを取得するアルゴリズムを指定します。
       * 任意の処理を記述してください。
       */
      def resolveUser(id: Id): Option[User] = Account.findById(id)
    
      /**
       * ログインが成功した際に遷移する先を指定します。
       */
      def loginSucceeded[A](request: Request[A]): PlainResult = Redirect(routes.Message.main)
    
      /**
       * ログアウトが成功した際に遷移する先を指定します。
       */
      def logoutSucceeded[A](request: Request[A]): PlainResult = Redirect(routes.Application.login)
    
      /**
       * 認証が失敗した場合に遷移する先を指定します。
       */
      def authenticationFailed[A](request: Request[A]): PlainResult = Redirect(routes.Application.login)
    
      /**
       * 認可(権限チェック)が失敗した場合に遷移する先を指定します。
       */
      def authorizationFailed[A](request: Request[A]): PlainResult = Forbidden("no permission")
    
      /**
       * 権限チェックのアルゴリズムを指定します。
       * 任意の処理を記述してください。
       */
      def authorize(user: User, authority: Authority): Boolean = 
        (user.permission, authority) match {
          case (Administrator, _) => true
          case (NormalUser, NormalUser) => true
          case _ => false
        }
    
    }
    ```

1. 次にログイン、ログアウトを行う `Controller` を作成します。
   この `Controller` に、先ほど作成した `AuthConfigImpl` トレイトと、
   `jp.t2v.lab.play20.auth.LoginLogout` トレイトを mixin します。

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
       * gotoLogoutSucceeded メソッドは PlainResult を返しますので、
       * 以下のように任意の処理を追加することもできます。
       * 
       *   gotoLogoutSucceeded.flashing(
       *     "success" -> "You've been logged out"
       *   )
       */
      def logout = Action { implicit request =>
        // do something...
        gotoLogoutSucceeded
      }
    
      /**
       * ログイン処理では認証が成功した場合、
       * gotoLoginSucceeded メソッドを呼び出した結果を返して下さい。
       * gotoLoginSucceeded メソッドも gotoLogoutSucceeded と同じく PlainResult を返しますので、
       * 任意の処理を追加することも可能です。
       */
      def authenticate = Action { implicit request =>
        loginForm.bindFromRequest.fold(
          formWithErrors => BadRequest(html.login(formWithErrors)),
          user => gotoLoginSucceeded(user.get.id)
        )
      }
    
    }
    ```

1. 最後は、好きな `Controller` に 先ほど作成した `AuthConfigImpl` トレイトと
   `jp.t2v.lab.play20.auth.Auth` トレイト を mixin すれば、認証/認可の仕組みを導入することができます。

    ```scala
    object Message extends Controller with Auth with AuthConfigImpl {
    
      // authorizedAction は 第一引数に権限チェック用の Authority を取り、
      // 第二引数に User => Request[AnyContent] => Result な関数を取り、
      // Action を返します。
    
      def main = authorizedAction(NormalUser) { user => implicit request =>
        val title = "message main"
        Ok(html.message.main(title))
      }
    
      def list = authorizedAction(NormalUser) { user => implicit request =>
        val title = "all messages"
        Ok(html.message.list(title))
      }
    
      def detail(id: Int) = authorizedAction(NormalUser) { user => implicit request =>
        val title = "messages detail "
        Ok(html.message.detail(title + id))
      }
    
      // このActionだけ、Administrator でなければ実行できなくなります。
      def write = authorizedAction(Administrator) { user => implicit request =>
        val title = "write message"
        Ok(html.message.write(title))
      }
    
    }
    ```


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

  type Authority = User => Boolean

  def authorize(user: User, authority: Authority): Boolean = authority(user)

}
```

```scala
object Application extends Controller with Auth with AuthConfigImpl {

  private def sameAuthor(messageId: Int)(account: Account): Boolean =
    Message.getAuther(messageId) == account

  def edit(messageId: Int) = authorizedAction(sameAuthor(messageId)) { user => request =>
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

  def authenticationFailed[A](request: Request[A]): PlainResult = 
    Redirect(routes.Application.login).withSession("access_uri" -> request.uri)

  def loginSucceeded[A](request: Request[A]): PlainResult = {
    val uri = request.session.get("access_uri").getOrElse(routes.Message.main.url)
    request.session - "access_uri"
    Redirect(uri)
  }

}
```

### ログイン状態と未ログイン状態で表示を変える

トップページなどにおいて、未ログイン状態でも画面を正常に表示し、
ログイン状態であればユーザ名などを表示する、といったことがしたい場合、
以下のように `optionalUserAction` を使用することで実現することができます。

```scala
object Application extends Controller with Auth with AuthConfigImpl {

  // maybeUser is an Option[User] instance.
  def index = optionalUserAction { maybeUser => request =>
    val user: User = maybeUser.getOrElse(GuestUser)
    Ok(html.index(user))
  }

}
```


### 他のAction操作と合成する

例えば、CSRF対策で各Actionでトークンのチェックをしたい、としましょう。

全てのActionで毎回チェックロジックを書くのは大変なので、普通はこんなActionの拡張をすると思います。

```scala
object Application extends Controller {

  // Token の発行処理は省略

  val tokenForm = Form("token" -> text)

  private def validateToken(request: Request[AnyContent]): Boolean = (for {
    tokenInForm <- tokenForm.bindFromRequest(request).value
    tokenInSession <- request.session.get("token")
  } yield tokenInForm == tokenInSession).getOrElse(false)

  private def validAction(f: Request[AnyContent] => Result) = Action { request =>
    if (validateToken(request)) f(request)
    else BadRequest
  }

  def page1 = validAction { request =>
    // do something
    Ok(html.page1("result"))
  }

  def page2 = validAction { request =>
    // do something
    Ok(html.page2("result"))
  }

}
```

この validateToken に認証/認可の仕組みを組み込むにはどうすればいいでしょうか？

`authorizedAction` メソッドの代わりに `authorized` メソッドを使うことで簡単に実現ができます。

```scala
object Application extends Controller with Auth with AuthConfigImpl {

  // Token の発行処理は省略

  val tokenForm = Form("token" -> text)

  private def validateToken(implicit request: Request[AnyContent]): Boolean = (for {
    tokenInForm <- tokenForm.bindFromRequest(request).value
    tokenInSession <- request.session.get("token")
  } yield tokenInForm == tokenInSession).getOrElse(false)

  private authAndValidAction(authority: Authority)(f: User => Request[AnyContent] => Result) =
    Action { implicit request =>
      (for {
        user <- authorized(authority).right
        _    <- Either.cond(validateToken, (), BadRequest).right
      } yield f(user)(request)).merge
    }

  def page1 = authAndValidAction { user => request =>
    // do something
    Ok(html.page1("result"))
  }

  def page2 = authAndValidAction { user => request =>
    // do something
    Ok(html.page2("result"))
  }

}
```

この例だけでは簡単さが実感できないかもしれません。
ではこれに更に pjax によって動的に Template を切り替えたいといったらどうでしょう？

その場合でも柔軟に取込むことができます。

```scala

  private type Template = String => Html
  private def pjax(implicit request: Request[AnyContent]): Template = {
    if (request.headers.keys("X-PJAX")) {
      html.pjaxTemplate.apply
    } else {
      val displayValues = DomainLogic.getDisplayValues()
      html.fullTemplate.apply(displayValues)
    }
  }

  private complexAction(authority: Authority)(f: User => Template => Request[AnyContent] => Result) =
    Action { implicit request =>
      (for {
        user     <- authorized(authority).right
        _        <- Either.cond(validateToken, (), BadRequest).right
        template <- Right(pjax).right
      } yield f(user)(template)(request)).merge
    }

  def page1 = complexAction { user => template => request =>
    // do something
    Ok(template("result"))
  }

  def page2 = complexAction { user => template => request =>
    // do something
    Ok(template("result"))
  }
```

このようにどんどん Action に対して操作の合成を行っていくことができます。


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
以下のように `RelationResolver` の実装を `CookieRelationResolver` 切り替えることでステートレスにすることができます。

```scala
trait AuthConfigImpl extends AuthConfig {

  // 他の設定省略

  override def resolver[A](implicit request: Request[A]) =
    new CookieRelationResolver[Id, A](request)

}
```

`RelationResolver` は SessionID および UserID を紐付ける責務を負っています。
この実装を切り替えることで、例えば RDBMS に認証情報を登録するといった事も可能です。

なお、`CookieRelationResolver` ではSessionタイムアウトは未サポートとなっています。


サンプルアプリケーション
---------------------------------------

1. `git clone https://github.com/t2v/play20-auth.git`
1. `cd play20-auth`
1. `play`
1. `run`
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

このモジュールは Play2.0 の [Cache API](http://www.playframework.org/documentation/2.0/ScalaCache) を利用しています。

標準実装の [Ehcache](http://ehcache.org) では、サーバを分散させた場合に正しく認証情報を扱えない場合があります。

サーバを分散させる場合には [Memcached Plugin](https://github.com/mumoshu/play2-memcached) 等を利用してください。


ライセンス
---------------------------------------

このモジュールは Apache Software License, version 2 の元に公開します。

詳しくは `LICENSE` ファイルを参照ください。

