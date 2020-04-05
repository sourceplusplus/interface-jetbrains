The Application API allows you to create and retrieve Source++ augmented applications. You can use this API to automatically synchronize the data in Source++ with your own system, without any human interference.

**PUT** and **POST** methods are used to create and update data. The following PUT and POST methods are available:

| Method | Address                                                      | Description                                               |
| ------ | ------------------------------------------------------------ | --------------------------------------------------------- |
| POST   | [/applications](#create-application)                         | Create new application                                    |
| PUT    | [/applications/:appUuid](#update-application)                | Update existing application                               |
| PUT    | [/applications/:appUuid/subscribers/:subscriberUuid/refresh](#refresh-subscriber-subscriptions) | Refresh subscriber's active source artifact subscriptions |

**GET** methods are used to fetch data. The following GET methods are available:

| Method | Address                                                      | Description                        |
| ------ | ------------------------------------------------------------ | ---------------------------------- |
| GET    | [/applications/:appUuid](#get-application)                   | Get application information        |
| GET    | [/applications/:appUuid/subscriptions](#get-application-subscriptions) | Get application-wide subscriptions |

---------------------------------------------------------------------------------------------------------------------------------


# Create Application
**Endpoint [POST]**: [/applications](https://api.sourceplusplus.com/v1/applications)

**Description**: Create new application.

**Request [CreateApplicationRequest]**:
```json
{
    "app_name": "red-fox"
}
```

**Response [Application]**:
```json
{
   "app_uuid":"f03b9e65-2ef8-46a1-a8c9-6478cdd79ecf",
   "app_name":"red-fox",
   "create_date":1510808253.041000000
}
```

# Update Application
**Endpoint [POST]**: [/applications/:appUuid](https://api.sourceplusplus.com/v1/applications/:appUuid)

**Description**: Update existing application.

**Request [Application]**:
```json
{
    "agent_config":{
      "application":{
         "application_source_code":{
            "packages":[
               "com/company/.*"
            ]
         }
      }
   }
}
```

**Response [Application]**:
```json
{
    "app_uuid": "fd4ba998-7ecc-40f5-b9cb-1880a3f1c93b",
    "app_name": "red-fox",
    "create_date": 1510808253.041000000,
    "agent_config":{
      "application":{
         "application_source_code":{
            "packages":[
               "com/company/.*"
            ]
         }
      }
   }
}
```

# Refresh Subscriber Subscriptions
**Endpoint [POST]**: [/applications/:appUuid/subscribers/:subscriberUuid/refresh](https://api.sourceplusplus.com/v1/applications/:appUuid/subscribers/:subscriberUuid/refresh)

**Description**: Refresh subscriber's active source artifact subscriptions.

**Request**:
```
n/a
```

**Response**:
```
n/a (200 OK)
```

# Get Application
**Endpoint [GET]**: [/applications/:appUuid](https://api.sourceplusplus.com/v1/applications/:appUuid)

**Description**: Get application information.

**Request**:
```
n/a
```

**Response [Application]**:
```json
{
    "app_uuid": "fd4ba998-7ecc-40f5-b9cb-1880a3f1c93b",
    "app_name": "red-fox",
    "create_date": 1510808253.041000000,
    "agent_config":{
      "application":{
         "application_source_code":{
            "packages":[
               "com/company/.*"
            ]
         }
      }
   }
}
```

# Get Application Subscriptions
**Endpoint [GET]**: [/applications/:appUuid/subscriptions](https://api.sourceplusplus.com/v1/applications/:appUuid/subscriptions)

**Description**: Get application-wide subscriptions.

**Request**:
```
n/a
```

**Response ( SourceApplicationSubscription[] )**:
```json
[
  {
    "artifact_qualified_name": "com.company.TestClass.testMethod()",
    "subscribers": 1,
    "types": [
      "METRICS",
      "TRACES"
    ],
    "automatic_subscription": false
  }
]
```
