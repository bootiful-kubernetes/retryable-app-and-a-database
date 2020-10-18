content=$(pwd)/some/content
mkdir -p $content
echo "<h1> Hello, world</h1>" > $content/index.html
docker run --name nginx-${RANDOM} -p 8080:80 -v  ${content}:/usr/share/nginx/html:ro -d nginx
