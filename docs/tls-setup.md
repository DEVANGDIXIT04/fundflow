# TLS with DuckDNS + Let's Encrypt (free)

## 1. DuckDNS subdomain

1. Sign in at <https://www.duckdns.org> (GitHub login works).
2. Create a subdomain, e.g. `fundflow` → `fundflow.duckdns.org`.
3. Point it at the EC2 public IP (paste the IP, click *update ip*).
4. EC2 public IPs change on stop/start — either keep the instance running,
   attach an Elastic IP (free **while attached**), or add a cron job on the
   instance that re-posts the IP to DuckDNS:

   ```
   echo '*/5 * * * * curl -s "https://www.duckdns.org/update?domains=fundflow&token=<YOUR_TOKEN>&ip=" >/dev/null' | crontab -
   ```

## 2. NGINX reverse proxy

```
sudo cp nginx/fundflow.conf /etc/nginx/conf.d/fundflow.conf
sudo sed -i 's/fundflow.duckdns.org/<yours>.duckdns.org/' /etc/nginx/conf.d/fundflow.conf
sudo nginx -t && sudo systemctl reload nginx
```

Check: `http://<yours>.duckdns.org/actuator/health` should answer over plain
HTTP through the proxy.

## 3. Let's Encrypt certificate

```
sudo dnf install -y certbot python3-certbot-nginx
sudo certbot --nginx -d <yours>.duckdns.org --redirect -m you@example.com --agree-tos
```

certbot edits the NGINX config in place: adds the `listen 443 ssl` server,
installs the cert, and (because of `--redirect`) 301s all HTTP to HTTPS.

Renewal is automatic (`certbot renew` systemd timer). Verify:

```
sudo certbot renew --dry-run
```

## Result

`https://<yours>.duckdns.org/swagger-ui/index.html` — TLS-terminated at NGINX,
proxied to the Spring Boot container on 127.0.0.1:8080, which is not exposed
publicly (the security group only opens 22/80/443).
