FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /src
COPY bots/FluxVisualsBots.csproj bots/
RUN dotnet restore bots/FluxVisualsBots.csproj
COPY bots/ bots/
RUN dotnet publish bots/FluxVisualsBots.csproj -c Release -o /app/publish --no-restore

FROM mcr.microsoft.com/dotnet/aspnet:10.0
WORKDIR /app
COPY --from=build /app/publish .
COPY bots/entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh
EXPOSE 5001 5002
CMD ["sh", "/app/entrypoint.sh"]
