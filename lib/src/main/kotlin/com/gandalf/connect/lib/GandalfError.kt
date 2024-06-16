package com.gandalf.connect.lib

class GandalfError(message: String, val code: GandalfErrorCode) : Exception(message)

enum class GandalfErrorCode {
    DataKeyNotFound,
    InvalidPublicKey,
    InvalidService,
    InvalidRedirectURL,
    InvalidBaseURL
}
