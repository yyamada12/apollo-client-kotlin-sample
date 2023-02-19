# graphql sample
## 概要
kotlin + Spring Boot 構成で GraphQL Client を実装する  
まずは、Node.js で適当なGraphQL Serverを構築し、
そこに接続する形とする。

## 前提
動作確認環境
M1 Mac
Node.js v18.4.0
npm v8.11.0
java 11
kotlin 1.6.21


## 準備: GraphQL Server の構築

https://www.apollographql.com/docs/apollo-server/getting-started
に沿って作っていく

```
mkdir server
cd server

npm init --yes
npm install @apollo/server graphql
```

index.js
```
import { ApolloServer } from "@apollo/server";
import { startStandaloneServer } from "@apollo/server/standalone";

// A schema is a collection of type definitions (hence "typeDefs")
// that together define the "shape" of queries that are executed against
// your data.
const typeDefs = `#graphql
  # Comments in GraphQL strings (such as this one) start with the hash (#) symbol.

  # This "Book" type defines the queryable fields for every book in our data source.
  type Book {
    title: String
    author: String
  }

  # The "Query" type is special: it lists all of the available queries that
  # clients can execute, along with the return type for each. In this
  # case, the "books" query returns an array of zero or more Books (defined above).
  type Query {
    books: [Book]
  }
`;

const books = [
  {
    title: "The Awakening",
    author: "Kate Chopin",
  },
  {
    title: "City of Glass",
    author: "Paul Auster",
  },
];

// Resolvers define how to fetch the types defined in your schema.
// This resolver retrieves books from the "books" array above.
const resolvers = {
  Query: {
    books: () => books,
  },
};

// The ApolloServer constructor requires two parameters: your schema
// definition and your set of resolvers.
const server = new ApolloServer({
  typeDefs,
  resolvers,
});

// Passing an ApolloServer instance to the `startStandaloneServer` function:
//  1. creates an Express app
//  2. installs your ApolloServer instance as middleware
//  3. prepares your app to handle incoming requests
const { url } = await startStandaloneServer(server, {
  listen: { port: 4000 },
});

console.log(`🚀  Server ready at: ${url}`);
```

package.json
```
{
  "name": "server",
  "version": "1.0.0",
  "main": "index.js",
  "license": "MIT",
  "type": "module",
  "dependencies": {
    "@apollo/server": "^4.4.0",
    "graphql": "^16.6.0"
  }
}
```

サーバ起動
```
node index.js
```

localhost:4000 で起動するので、アクセスすると、 Apollo GraphQL Server の画面が表示される

以下のようなqueryを実行すると、booksが取得できることを確認できる
```
query Books {
  books {
    title
  }
}
```

queryの書き方は↓を参照
https://www.apollographql.com/docs/react/data/queries/

日本語だと↓とか
https://qiita.com/shunp/items/d85fc47b33e1b3a88167


## GraphQL Client の構築
いよいよGraphQL Clientを実装していく  
参考: https://www.apollographql.com/docs/kotlin

spring intializr で spring web のみを追加してprojectを作成した素のSpring Projectになっている


1: build.gradle.kts に Apollo GraphQL の plugin を追加
参考: https://www.apollographql.com/docs/kotlin/advanced/plugin-configuration/

```
plugins {
    ...

	// apollo graphql の plugin を追加
	id("com.apollographql.apollo").version("3.7.4")
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

...

// apollo graphql の plugin 設定
apollo {
	service("sample-service") {
		packageName.set("com.example")

		introspection {
			endpointUrl.set("http://localhost:4000/graphql/endpoint")
			schemaFile.set(file("src/main/graphql/com/example/schema.graphqls"))
		}

	}
}
```

 `./gradlew tasks` を実行すると、以下のように Apollo tasks が追加されているのが確認できる


```
Apollo tasks
------------
convertApolloSchema
downloadApolloSchema
downloadSample-serviceApolloSchemaFromIntrospection
generateApolloSources - Generate Apollo models for all services
generateSample-serviceApolloSchema - Generate Apollo schema for sample-service
generateSample-serviceApolloSources - Generate Apollo models for sample-service GraphQL queries
generateSample-serviceApolloUsedCoordinates - Generate Apollo used coordinates for sample-service GraphQL queries
pushApolloSchema
```

taskがたくさんあるように見えるが、実質以下の4種類しかない
- convert schema
  - json の schema を SDL(Schema Definition Language) の schema に変換する
- download schema
  - GraphQL Server から SDL schema を download する
- generate souces
  - schema と query からソースコードを自動生成する
- push schema 
  - schema を GraphQL Server に push する



node.js で作成した GraphQL Server が立ち上がっている状態で、
`./gradlew downloadApolloSchema` を実行すると、`src/main/graphql/com/example/schema.graphqls` に schema ファイルが生成される

```
type Book {
  title: String
  author: String
}

type Query {
  books: [Book]
}

schema {
  query: Query
}
```


2: query を作成する
実行したい query を `src/main/graphql/com/example/` 配下に配置する

getBooks.graphql
```
query Books {
  books {
    title
  }
}
```

3: client コードを生成する
`./gradlew generateApolloSources` を実行すると、 `build/generated/source/apollo/main/sample-service/com/example/BooksQuery.java` にコードが自動生成されるのが確認できる

4: build.gradle.kts に Apollo GraphQL のライブラリへの依存を追加する
先ほどは plugin を追加したが、今回はライブラリを追加する

build.gradle.kts
```
dependencies {
    ...

    // apollo graphql の ライブラリを追加
	implementation("com.apollographql.apollo:apollo-runtime:2.5.14")

	// 非同期通信を扱うためのライブラリ
	implementation("com.apollographql.apollo:apollo-rx3-support:2.5.14")
}
```

5: 生成されたclientコードを使って処理を行う
