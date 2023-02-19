package com.example.client

import com.apollographql.apollo3.ApolloClient
import com.example.BooksQuery
import kotlinx.coroutines.runBlocking
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component


@SpringBootApplication
class ClientApplication

fun main(args: Array<String>) {
	runApplication<ClientApplication>(*args)
}


@Component
class SampleApplicationRunner : ApplicationRunner {
	override fun run(args: ApplicationArguments) {
		val apolloClient = ApolloClient.Builder().serverUrl("http://localhost:4000/graphql/endpoint").build()

		val response = runBlocking {
			 apolloClient.query(BooksQuery()).execute()
		}

		println("books:${response.data?.books}")

	}
}