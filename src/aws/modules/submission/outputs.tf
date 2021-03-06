output "store" {
  value = module.submission_store.bucket_name
}

output "store_id" {
  value = module.submission_store.bucket_id
}

output "store_arn" {
  value = module.submission_store.bucket_arn
}

output "function" {
  value = module.submission_lambda.lambda_function_name
}

output "version" {
  value = module.submission_lambda.version
}

output "endpoint" {
  value = module.submission_gateway.api_endpoint
}

output "gateway_id" {
  value = module.submission_gateway.api_gateway_id
}
