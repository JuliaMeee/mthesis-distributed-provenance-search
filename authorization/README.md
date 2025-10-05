To check if you as a user have access to a bundle, use  
```
curl -X POST "http://authorization-service:9000/api/check" \
-H "Authorization: Bearer <access_token>" \
-H "Content-Type: application/json" -d '{"bundleId":"<bundleId>"}'
```

To check whether an organization has access to a bundle, use  
```
curl -X POST http://authorization-service:9000/api/checkOrgAccess \
  -H "Content-Type: application/json" -d '{"bundleId":"<bundleId>","orgId":"<orgId>"}'
```

Access rights of a user are decided based on his assigned organization.

The service uses a db to store which organization has access to which bundle. It is seeded here:  
`src/main/java/cz/muni/xmichalk/persistence/DbSeeder.java`  
If there is no row in db with given bundleId and organization, access is granted or denied based on the property `isAccessGrantedByDefault` in `application.yaml`.

To get a bearer token:
1. Go to this url, change the value of the organization parameter at the end to the one you want to be associated with:
`https://dev-xxx.eu.auth0.com/authorize?response_type=code&client_id=Z8H8tZDcun9Mz48AWeqH1IffTBiwM9Al&redirect_uri=http://localhost:9000/callback&scope=openid%20profile%20email&audience=http://dist-prov-search&organization=org1`
2. After login it will redirect you to callback url with a code parameter.
3. Paste your code in this curl:  
```
curl --request POST \
  --url https://dev-xxx.eu.auth0.com/oauth/token \
  --header 'content-type: application/json' \
  --data '{
    "grant_type": "authorization_code",
    "client_id": "Z8H8tZDcun9Mz48AWeqH1IffTBiwM9Al",
    "client_secret": "ecgV5Mj-wxd2GWz4iY1aOVWDk_nmyuZqvnKc9_te2ESVKpARTWCSghJGYbwBV_DX",
    "code": "<your-code-here>",
    "redirect_uri": "http://localhost:9000/callback"
  }'
  ```
4. The curl returns a response containing an access_token.

Current organizations:
- org1
- org2
- org3