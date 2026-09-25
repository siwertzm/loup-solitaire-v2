UPDATE public.chapitre
SET text = replace(
    text,
    'Bois des Brumes ; c''est là que s''est installée',
    'Bois des Brumes. C''est là que s''est installée'
)
WHERE id = 272;