# Project Overview:

The purpose of this project is to fetch data from an external API and return the
short forecasting details like _Patchy Fog then Sunny_, _Clear_, _Sunny_ etc. along with
the temperature category according to temperature levels.

## Setup Instructions:

1. Clone this project using `git clone ${URL}`
2. Run the project using `sbt run`. At first, it should take sometime as it will download
   dependencies. But after the first run, it will be much faster

## Execution Guide: 
When you run the project, the server will serve on `localhost:8080`. And there is only one enpoint
to make request and it's a `POST` request. You can make a post request to aforementioned localhost URL
like below

```json
{
   "latitude": 39.7456,
   "longitude": -97.0892
}
```

## Design Decisions:
This is implemented using non-block async API calls with the help of `Akka HTTP`

The following categorization is used for temperature categorization

```
Very Cold: Below 0°C
Cold: 0°C to 10°C
Cool: 10°C to 15°C
Moderate: 15°C to 25°C
Warm: 25°C to 30°C
Hot: 30°C to 35°C
Very Hot: Above 35°C
```
