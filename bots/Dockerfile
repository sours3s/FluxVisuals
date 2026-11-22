FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /src
COPY bots/FluxVisualsBots.csproj bots/
RUN dotnet restore bots/FluxVisualsBots.csproj
COPY bots/ bots/
RUN dotnet publish bots/FluxVisualsBots.csproj -c Release -o /app/publish --no-restore

FROM mcr.microsoft.com/dotnet/aspnet:10.0
WORKDIR /app
COPY --from=build /app/publish .
EXPOSE 10000
CMD ["sh", "-c", "dotnet FluxVisualsBots.dll --urls http://0.0.0.0:${PORT:-10000}"]
